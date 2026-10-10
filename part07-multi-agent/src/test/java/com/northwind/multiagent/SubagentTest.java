package com.northwind.multiagent;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.multiagent.returns.Returns.Customer;
import com.northwind.multiagent.returns.Returns.OrderFacts;
import com.northwind.multiagent.returns.Returns.ReturnOutcome;
import com.northwind.multiagent.returns.Returns.ReturnRequest;
import com.northwind.multiagent.returns.Returns.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 49 and 51: subagents, shared blackboards and failure. */
class SubagentTest extends EmbabelMockitoIntegrationTest {

    private Agent agent(String name) {
        return agentPlatform.agents().stream().filter(a -> a.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void deskDelegatesToTheEligibilitySubagent() {
        ReturnOutcome standard = AgentInvocation.create(agentPlatform, ReturnOutcome.class)
                .invoke(new ReturnRequest("NW-10442", "BOOT-W9", "too small", 45), new Customer("C-1001", Tier.STANDARD));
        ReturnOutcome gold = AgentInvocation.create(agentPlatform, ReturnOutcome.class)
                .invoke(new ReturnRequest("NW-10442", "BOOT-W9", "too small", 45), new Customer("C-1002", Tier.GOLD));

        System.out.println("standard: " + standard);
        System.out.println("gold:     " + gold);
        assertThat(standard.status()).isEqualTo("DECLINED");
        assertThat(gold.status()).isEqualTo("APPROVED");
    }

    @Test
    void theSubagentsResultReturnsButItsIntermediatesStayInside() throws Exception {
        AgentProcess process = agentPlatform.createAgentProcessFrom(agent("ReturnsDeskAgent"), new ProcessOptions(),
                new ReturnRequest("NW-10442", "BOOT-W9", "damaged", 70), new Customer("C-1001", Tier.STANDARD));
        agentPlatform.start(process).get();

        List<String> history = process.getHistory().stream().map(h -> h.getActionName()).toList();
        List<String> objects = process.getObjects().stream().map(o -> o.getClass().getSimpleName()).toList();
        System.out.println("status:  " + process.getStatus());
        System.out.println("history: " + history);
        System.out.println("objects: " + objects);

        assertThat(process.getStatus()).isEqualTo(AgentProcessStatusCode.COMPLETED);
        assertThat(history).hasSize(2);                                   // the parent's own two actions
        assertThat(process.last(OrderFacts.class)).isNull();             // the subagent's working data stayed inside
        assertThat(process.last(ReturnOutcome.class).status()).isEqualTo("APPROVED");
    }

    @Test
    void anExpectedFailureIsADecisionNotACrash() {
        ReturnOutcome unknown = AgentInvocation.create(agentPlatform, ReturnOutcome.class)
                .invoke(new ReturnRequest("NW-99999", "X", "too small", 3), new Customer("C-1001", Tier.STANDARD));

        System.out.println("unknown order: " + unknown);
        assertThat(unknown.status()).isEqualTo("DECLINED");
        assertThat(unknown.message()).contains("could not find order NW-99999");
    }

    @Test
    void missingContextStopsTheParentBeforeItDelegates() throws Exception {
        AgentProcess process = agentPlatform.createAgentProcessFrom(agent("ReturnsDeskAgent"), new ProcessOptions(),
                new ReturnRequest("NW-10442", "BOOT-W9", "too small", 3));        // no Customer bound
        agentPlatform.start(process).get();

        System.out.println("missing customer: status=" + process.getStatus() + ", history=" + process.getHistory().size());
        assertThat(process.getStatus()).isEqualTo(AgentProcessStatusCode.STUCK);  // the PARENT cannot plan
        assertThat(process.getHistory()).isEmpty();                              // the subagent never started
    }
}
