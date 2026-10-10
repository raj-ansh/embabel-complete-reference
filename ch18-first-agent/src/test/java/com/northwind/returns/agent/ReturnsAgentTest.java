package com.northwind.returns.agent;

import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.northwind.returns.domain.CustomerReply;
import com.northwind.returns.domain.ReturnDecision;
import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import com.northwind.returns.service.OrderService;
import com.northwind.returns.service.ReturnsPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Each action is an ordinary method: test it directly, with fakes for the model. */
class ReturnsAgentTest {

    private final ReturnsAgent agent = new ReturnsAgent(new OrderService(), new ReturnsPolicy());

    @Test
    void understandDelimitsTheMessageAndUsesTemperatureZero() {
        var context = FakeOperationContext.create();
        context.expectResponse(new ReturnRequest("NW-10442", "TENT-2P", ReturnReason.DAMAGED, "pole snapped"));

        ReturnRequest request = agent.understand(new UserInput("My tent pole snapped! Order NW-10442"), context);

        assertThat(request.reason()).isEqualTo(ReturnReason.DAMAGED);
        var invocation = ((FakePromptRunner) context.promptRunner()).getLlmInvocations().getFirst();
        assertThat(invocation.getPrompt()).contains("<message>\nMy tent pole snapped!");
        assertThat(invocation.getInteraction().getLlm().getTemperature()).isEqualTo(0.0);
    }

    @Test
    void findOrderAndDecideUseNoModel() {
        var request = new ReturnRequest("NW-10442", "TENT-2P", ReturnReason.DAMAGED, null);

        var order = agent.findOrder(request);
        ReturnDecision decision = agent.decide(request, order);

        assertThat(decision.outcome()).isEqualTo(ReturnDecision.Outcome.APPROVED);
        assertThat(decision.freeReturnLabel()).isTrue();
    }

    @Test
    void unknownOrderFailsTheAction() {
        assertThatThrownBy(() -> agent.findOrder(new ReturnRequest("NW-99999", "X", ReturnReason.DAMAGED, null)))
                .hasMessageContaining("Unknown order NW-99999");
    }

    @Test
    void replyIsGivenTheDecisionNotAskedToMakeIt() {
        var context = FakeOperationContext.create();
        context.expectResponse(new CustomerReply("Your return is approved", "Hi Asha, …"));
        var request = new ReturnRequest("NW-10442", "TENT-2P", ReturnReason.DAMAGED, null);
        var order = agent.findOrder(request);

        agent.reply(request, order, agent.decide(request, order), context);

        String prompt = ((FakePromptRunner) context.promptRunner()).getLlmInvocations().getFirst().getPrompt();
        assertThat(prompt).contains("Decision: APPROVED", "Refund amount: 189.00", "do not change it");
    }
}
