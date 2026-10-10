package com.northwind.core;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.ActionInvocation;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.core.actions.WarrantyAgent;
import com.northwind.core.actions.WarrantyAgent.ClaimOutcome;
import com.northwind.core.actions.WarrantyAgent.LoyaltyStatus;
import com.northwind.core.actions.WarrantyAgent.WarrantyClaim;
import com.northwind.core.agents.SloganAgentConfiguration.Slogan;
import com.northwind.core.blackboard.PriceComparisonAgent.AuditNote;
import com.northwind.core.blackboard.PriceComparisonAgent.PriceGap;
import com.northwind.core.blackboard.PriceComparisonAgent.ProductCode;
import com.northwind.core.conditions.ShippingUpgradeAgent.CartTotal;
import com.northwind.core.conditions.ShippingUpgradeAgent.ShippingDecision;
import com.northwind.core.domain.LoyaltyAdvisorAgent.BenefitsExplanation;
import com.northwind.core.domain.LoyaltyMember;
import com.northwind.core.goals.OrderInquiryAgent.DeliveryEstimate;
import com.northwind.core.goals.OrderInquiryAgent.InvoiceCopy;
import com.northwind.core.goals.OrderInquiryAgent.DeliveryQuestion;
import com.northwind.core.goals.OrderInquiryAgent.InvoiceRequest;
import com.northwind.core.process.GiftCardAgent.GiftCard;
import com.northwind.core.process.GiftCardAgent.GiftCardOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CoreConceptsIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    WarrantyAgent.RepairShopClient repairShop;

    private static java.util.List<String> actionNames(com.embabel.agent.core.AgentProcess process) {
        return process.getHistory().stream().map(ActionInvocation::getActionName)
                .map(n -> n.substring(n.lastIndexOf('.') + 1)).toList();
    }

    // ---- Chapter 22: DSL agents are deployed like annotated ones
    @Test
    void dslAgentIsDeployedAndRuns() {
        whenCreateObject(p -> p.contains("slogan"), Slogan.class).thenReturn(new Slogan("Gear up for golden trails"));

        assertThat(agentPlatform.agents()).anyMatch(a -> a.getName().equals("SloganAgent"));
        assertThat(AgentInvocation.create(agentPlatform, Slogan.class).invoke(new UserInput("go")).text())
                .isEqualTo("Gear up for golden trails");
    }

    // ---- Chapter 23: SomeOf routing, optional inputs, transient retries
    @Test
    void someOfRoutesToTheMatchingAction() {
        var recent = new WarrantyClaim("W-1", "TENT-2P", LocalDate.of(2026, 3, 1), false);
        var misuse = new WarrantyClaim("W-2", "TENT-2P", LocalDate.of(2026, 3, 1), true);

        var replaced = AgentInvocation.create(agentPlatform, ClaimOutcome.class).run(recent);
        var rejected = AgentInvocation.create(agentPlatform, ClaimOutcome.class).run(misuse);

        assertThat(actionNames(replaced)).containsExactly("route", "replace");
        assertThat(actionNames(rejected)).containsExactly("route", "reject");
    }

    @Test
    void optionalInputIsUsedWhenPresent() {
        var older = new WarrantyClaim("W-3", "BOOT-W9", LocalDate.of(2024, 9, 1), false);

        ClaimOutcome plain = AgentInvocation.create(agentPlatform, ClaimOutcome.class).invoke(older);
        ClaimOutcome gold = AgentInvocation.create(agentPlatform, ClaimOutcome.class)
                .invoke(older, new LoyaltyStatus(true));

        assertThat(plain.outcome()).isEqualTo("REPAIR");
        assertThat(gold.outcome()).isEqualTo("REPAIR (priority)");
        assertThat(gold.daysToResolve()).isLessThan(plain.daysToResolve());
    }

    @Test
    void transientFailuresAreRetried() {
        repairShop.failNextCalls(2);
        var older = new WarrantyClaim("W-4", "BOOT-W9", LocalDate.of(2024, 9, 1), false);

        ClaimOutcome outcome = AgentInvocation.create(agentPlatform, ClaimOutcome.class).invoke(older);

        assertThat(outcome.outcome()).startsWith("REPAIR");
        assertThat(repairShop.calls()).isEqualTo(3);           // two timeouts, then success
        repairShop.failNextCalls(0);
    }

    // ---- Chapter 24: the requested type selects the goal
    @Test
    void theInputDeterminesWhichGoalIsReachable() {
        var delivery = AgentInvocation.create(agentPlatform, DeliveryEstimate.class).run(new DeliveryQuestion("NW-10442"));
        var invoice = AgentInvocation.create(agentPlatform, InvoiceCopy.class).run(new InvoiceRequest("NW-10442"));

        assertThat(actionNames(delivery)).containsExactly("loadForDelivery", "estimateDelivery");
        assertThat(actionNames(invoice)).containsExactly("loadForInvoice", "invoice");
        assertThat(delivery.resultOfType(DeliveryEstimate.class).eta()).isEqualTo(LocalDate.of(2026, 10, 9));
    }

    // ---- Chapter 25: conditions choose between actions with the same inputs
    @Test
    void conditionsSelectTheAction() {
        ShippingDecision big = AgentInvocation.create(agentPlatform, ShippingDecision.class)
                .invoke(new CartTotal(new BigDecimal("320.00"), true));
        ShippingDecision small = AgentInvocation.create(agentPlatform, ShippingDecision.class)
                .invoke(new CartTotal(new BigDecimal("80.00"), true));
        ShippingDecision noExpress = AgentInvocation.create(agentPlatform, ShippingDecision.class)
                .invoke(new CartTotal(new BigDecimal("320.00"), false));

        assertThat(big.offer()).isEqualTo("FREE_EXPRESS");
        assertThat(small.offer()).isEqualTo("STANDARD");
        assertThat(noExpress.offer()).isEqualTo("STANDARD");
    }

    // ---- Chapter 26: domain-object tools are offered to the model for that call only
    @Test
    void domainObjectToolsAreAttachedToTheCall() {
        whenCreateObject(p -> p.contains("<question>"), BenefitsExplanation.class,
                interaction -> interaction.getTools().stream()
                        .anyMatch(t -> t.getDefinition().getName().equals("loyaltyDiscountPercent")))
                .thenReturn(new BenefitsExplanation("You get 6% off."));

        var answer = AgentInvocation.create(agentPlatform, BenefitsExplanation.class)
                .invoke(new UserInput("What discount do I get?"), new LoyaltyMember("M-1", "Asha", 4200, 6));

        assertThat(answer.text()).isEqualTo("You get 6% off.");
        assertThat(new LoyaltyMember("M-1", "Asha", 4200, 6).loyaltyDiscountPercent()).isEqualByComparingTo("6.0");
    }

    // ---- Chapter 28: binding by name, and objects bound through the context
    @Test
    void namedBindingsKeepTwoPricesApart() {
        var process = AgentInvocation.create(agentPlatform, PriceGap.class).run(new ProductCode("TENT-2P"));

        PriceGap gap = process.resultOfType(PriceGap.class);
        assertThat(gap.difference()).isEqualByComparingTo("10.00");
        assertThat(gap.weAreCheaper()).isTrue();
        assertThat(process.last(AuditNote.class).text()).contains("TENT-2P");
    }

    // ---- Chapter 30: a StuckHandler rescues a process that cannot plan
    @Test
    void stuckHandlerSuppliesAMissingObjectAndReplans() {
        GiftCard card = AgentInvocation.create(agentPlatform, GiftCard.class)
                .invoke(new GiftCardOrder("Mateo", new BigDecimal("50.00")));

        assertThat(card.design()).isEqualTo("classic-forest");
    }
}
