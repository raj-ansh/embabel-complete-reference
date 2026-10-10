package com.northwind.multiagent;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.multiagent.returns.Returns.Customer;
import com.northwind.multiagent.returns.Returns.Tier;
import com.northwind.multiagent.service.Service.GiftIdeas;
import com.northwind.multiagent.service.Service.GiftRequest;
import com.northwind.multiagent.service.Service.Intent;
import com.northwind.multiagent.service.Service.ProductAdvice;
import com.northwind.multiagent.service.Service.SupportReply;
import com.northwind.multiagent.service.Service.Triage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapters 49 and 50: a supervisor routing to specialists, and a specialist offered as a tool. */
class SupervisorTest extends EmbabelMockitoIntegrationTest {

    private static final Customer ASHA = new Customer("C-1001", Tier.GOLD);

    private SupportReply ask(String message) {
        return AgentInvocation.create(agentPlatform, SupportReply.class).invoke(new UserInput(message), ASHA);
    }

    @Test
    void returnsAreRoutedThroughTwoLevelsOfSpecialists() {
        whenCreateObject(p -> p.contains("<message>") && p.contains("boots"), Triage.class)
                .thenReturn(new Triage(Intent.RETURN, "NW-10442", "too small", 45, null, "Return boots"));

        SupportReply reply = ask("I'd like to return the boots from NW-10442, they're too small. Got them 45 days ago.");

        System.out.println("return reply:  " + reply);
        assertThat(reply.handledBy()).isEqualTo("returns-desk");
        assertThat(reply.text()).contains("60-day");                     // Gold window, decided two levels down
    }

    @Test
    void productQuestionsGoToTheAdvisor() {
        whenCreateObject(p -> p.contains("<message>") && p.contains("tent"), Triage.class)
                .thenReturn(new Triage(Intent.PRODUCT_QUESTION, null, null, null,
                        "Which tent for two people in the rain?", "Tent advice"));
        whenCreateObject(p -> p.contains("<product-question>"), ProductAdvice.class)
                .thenReturn(new ProductAdvice("The Ridgeline 2P is fully waterproof and fits two.", List.of("TENT-2P")));

        SupportReply reply = ask("Which tent should I buy for two people? It rains a lot where we camp.");

        System.out.println("product reply: " + reply);
        assertThat(reply.handledBy()).isEqualTo("product-advisor");
    }

    @Test
    void anythingElseGoesToAPerson() {
        whenCreateObject(p -> p.contains("<message>") && p.contains("invoice"), Triage.class)
                .thenReturn(new Triage(Intent.OTHER, null, null, null, null, "Wants a VAT invoice"));

        SupportReply reply = ask("Can you send me a VAT invoice for my company?");

        System.out.println("other reply:   " + reply);
        assertThat(reply.handledBy()).isEqualTo("human:customer-care");
    }

    @Test
    void aReturnWithoutAnOrderNumberAlsoGoesToAPerson() {
        whenCreateObject(p -> p.contains("<message>") && p.contains("jacket"), Triage.class)
                .thenReturn(new Triage(Intent.RETURN, null, "wrong colour", null, null, "Return a jacket, no order number"));

        assertThat(ask("I want to return a jacket, wrong colour").handledBy()).isEqualTo("human:customer-care");
    }

    @Test
    void theRouteIsVisibleInTheProcessHistory() throws Exception {
        whenCreateObject(p -> p.contains("<message>"), Triage.class)
                .thenReturn(new Triage(Intent.RETURN, "NW-10442", "damaged", 12, null, "Damaged boots"));
        var supervisor = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("CustomerServiceSupervisor")).findFirst().orElseThrow();

        AgentProcess process = agentPlatform.createAgentProcessFrom(supervisor, new ProcessOptions(),
                new UserInput("The boots from NW-10442 arrived damaged"), ASHA);
        agentPlatform.start(process).get();

        List<String> route = process.getHistory().stream()
                .map(h -> h.getActionName().substring(h.getActionName().lastIndexOf('.') + 1)).toList();
        System.out.println("route: " + route);
        assertThat(route).containsExactly("triage", "toReturnRequest", "handleReturn", "replyAboutReturn", "deliver");
    }

    @Test
    void theConciergeSeesTheAdvisorAsATool() {
        whenCreateObject(p -> p.contains("<brief>"), GiftIdeas.class,
                interaction -> {
                    interaction.getTools().forEach(t -> System.out.println(
                            "tool: " + t.getDefinition().getName() + " -- " + t.getDefinition().getDescription()
                                    + "\n  schema: " + t.getDefinition().getInputSchema().toJsonSchema()));
                    return interaction.getTools().stream().anyMatch(t -> t.getDefinition().getName().equals("ProductAdvisor"));
                })
                .thenReturn(new GiftIdeas(List.of("Trailblazer Waterproof Boot (BOOT-W9)")));

        GiftIdeas ideas = AgentInvocation.create(agentPlatform, GiftIdeas.class)
                .invoke(new GiftRequest("My dad hikes every weekend in wet weather"));

        assertThat(ideas.ideas()).hasSize(1);
    }
}
