package com.northwind.support;

import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.support.domain.RefundLedger;
import com.northwind.support.domain.Support.BillingKind;
import com.northwind.support.domain.Support.Intent;
import com.northwind.support.domain.Support.TechnicalAnswer;
import com.northwind.support.domain.Support.Triage;
import com.northwind.support.web.SupportService;
import com.northwind.support.web.SupportService.ForbiddenException;
import com.northwind.support.web.SupportService.SupportView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P6: every route through the platform. Only model calls are stubbed: the triage, the technical
 * specialist and the final reply. Routing, specialists, policy, approval and the ledger all run for real.
 */
class SupportPlatformTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    SupportService support;

    @Autowired
    RefundLedger ledger;

    @BeforeEach
    void reset() {
        ledger.clear();
    }

    private void triage(String marker, Triage triage) {
        whenCreateObject(p -> p.contains("<message>") && p.contains(marker), Triage.class).thenReturn(triage);
    }

    /** The reply model must be given the specialist's facts; the stub only answers if they are in the prompt. */
    private void replyWhenFactsContain(String fact, String reply) {
        whenGenerateText(p -> p.contains("<facts>") && p.contains(fact)).thenReturn(reply);
    }

    @Test
    void orderQuestionsGoToOrderSupport() throws Exception {
        triage("boots", new Triage(Intent.ORDER, "NW-10442", null, null, null, "Where are my boots"));
        replyWhenFactsContain("is SHIPPED", "Good news: your Trailblazer boots (NW-10442) have shipped!");

        SupportView view = support.ask("C-1001", "Where are my boots? Order NW-10442");

        System.out.println("order:      " + view.reply());
        assertThat(view.reply().specialist()).isEqualTo("order-support");
    }

    @Test
    void anotherCustomersOrderIsNotFound() throws Exception {
        triage("NW-10442", new Triage(Intent.ORDER, "NW-10442", null, null, null, "Order status"));
        replyWhenFactsContain("No order NW-10442 was found on this account",
                "I couldn't find order NW-10442 on your account. Could you check the number?");

        SupportView view = support.ask("C-1002", "What's happening with NW-10442?");

        System.out.println("not theirs: " + view.reply());
        assertThat(view.reply().text()).contains("couldn't find");
    }

    @Test
    void technicalQuestionsGoToTechnicalSupportWithTheCareGuides() throws Exception {
        triage("waterproof", new Triage(Intent.TECHNICAL, null, null, null, "How do I re-waterproof my boots?", "Boot care"));
        whenCreateObject(p -> p.contains("<technical-question>"), TechnicalAnswer.class,
                interaction -> interaction.getTools().stream().anyMatch(t -> t.getDefinition().getName().equals("searchCareGuides")))
                .thenReturn(new TechnicalAnswer("Clean, dry naturally, then apply a wax or spray proofer.",
                        List.of("Re-proofing waterproof boots")));
        replyWhenFactsContain("spray proofer", "Clean them, let them dry naturally, then apply a wax or spray proofer.");

        SupportView view = support.ask("C-1001", "My boots aren't waterproof any more, what should I do?");

        System.out.println("technical:  " + view.reply());
        assertThat(view.reply().specialist()).isEqualTo("technical-support");
    }

    @Test
    void invoicesAndSmallRefundsAreHandledByBillingAlone() throws Exception {
        triage("invoice", new Triage(Intent.BILLING, "NW-10601", BillingKind.INVOICE, null, null, "Invoice for tent"));
        replyWhenFactsContain("invoice for NW-10601", "I've e-mailed the invoice for your tent order.");
        triage("partial", new Triage(Intent.BILLING, "NW-10442", BillingKind.REFUND, new BigDecimal("40.00"), null, "Partial refund"));
        replyWhenFactsContain("refund of $40.00", "I've refunded $40.00 for NW-10442.");

        SupportView invoice = support.ask("C-1001", "Can I have an invoice for NW-10601?");
        SupportView refund = support.ask("C-1001", "The boots had a scuff, can I get a partial refund of $40 on NW-10442?");

        System.out.println("invoice:    " + invoice.reply());
        System.out.println("refund:     " + refund.reply());
        assertThat(refund.status()).isEqualTo("COMPLETED");
        assertThat(ledger.issued()).singleElement().satisfies(r -> assertThat(r.approvedBy()).isEqualTo("policy"));
    }

    @Test
    void largeRefundsWaitForASupportLead() throws Exception {
        triage("tent", new Triage(Intent.BILLING, "NW-10601", BillingKind.REFUND, null, null, "Refund the tent"));
        replyWhenFactsContain("approved and issued", "Your refund of $329.00 has been approved and issued.");

        SupportView waiting = support.ask("C-1001", "The tent pole snapped on the first trip, I want a refund for NW-10601");
        System.out.println("waiting:    " + waiting.status() + " -- " + waiting.waitingFor());
        assertThat(waiting.status()).isEqualTo("WAITING");
        assertThat(ledger.issued()).isEmpty();                                  // nothing issued yet

        AgentProcess process = agentPlatform.getAgentProcess(waiting.conversationId());
        FormBindingRequest<?> pending = process.last(FormBindingRequest.class);
        System.out.println("form:       " + pending.getPayload().getControls());

        assertThatThrownBy(() -> support.decideRefund(waiting.conversationId(), "E-40", false, true, "ok"))
                .isInstanceOf(ForbiddenException.class);

        SupportView done = support.decideRefund(waiting.conversationId(), "L-2", true, true, "Manufacturing fault");
        System.out.println("approved:   " + done.reply());
        System.out.println("route:      " + process.getHistory().stream()
                .map(h -> h.getActionName().substring(h.getActionName().lastIndexOf('.') + 1)).toList());
        assertThat(done.status()).isEqualTo("COMPLETED");
        assertThat(ledger.issued()).singleElement().satisfies(r -> assertThat(r.approvedBy()).isEqualTo("L-2"));
    }

    @Test
    void refundsAboveWhatWasPaidAreRefused() throws Exception {
        triage("1000", new Triage(Intent.BILLING, "NW-10442", BillingKind.REFUND, new BigDecimal("1000"), null, "Big refund"));
        replyWhenFactsContain("must be between", "I can refund at most the $149.00 you paid for NW-10442.");

        SupportView view = support.ask("C-1001", "Refund me $1000 for NW-10442");

        System.out.println("refused:    " + view.reply());
        assertThat(ledger.issued()).isEmpty();
    }

    @Test
    void everythingElseGoesToAPerson() throws Exception {
        triage("franchise", new Triage(Intent.OTHER, null, null, null, null, "Wants to open a franchise"));
        replyWhenFactsContain("customer care team", "Thanks! I've passed this to our customer care team.");

        SupportView view = support.ask("C-1002", "How do I open a NorthWind franchise?");

        System.out.println("other:      " + view.reply());
        assertThat(view.reply().specialist()).isEqualTo("human:customer-care");
    }
}
