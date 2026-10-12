package com.northwind.production.unit;

import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.core.Action;
import com.embabel.agent.core.IoBinding;
import com.northwind.production.agent.IssueResolutionAgent;
import com.northwind.production.service.OrderService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Chapter 90: the planner orders actions by their inputs and outputs. Testing those declarations protects
 * properties such as "the ownership check runs before any model call", without running the agent.
 */
class AgentStructureTest {

    /** "it:com.northwind.production.domain.Issues$OrderFacts" -> "OrderFacts" */
    private static String typeName(IoBinding binding) {
        String value = binding.getValue();
        return value.substring(Math.max(value.lastIndexOf('.'), value.lastIndexOf('$')) + 1);
    }

    private static String shortName(Action action) {
        return action.getName().substring(action.getName().lastIndexOf('.') + 1);
    }

    @Test
    void triageNeedsTheOrderSoTheOwnershipCheckComesFirst() {
        var agent = new AgentMetadataReader().createAgentMetadata(new IssueResolutionAgent(mock(OrderService.class), Duration.ofSeconds(30)));

        for (Action action : agent.getActions()) {
            System.out.println(shortName(action) + ": "
                    + action.getInputs().stream().map(AgentStructureTest::typeName).sorted().collect(Collectors.joining(", "))
                    + " -> " + action.getOutputs().stream().map(AgentStructureTest::typeName).collect(Collectors.joining(", ")));
        }
        Action triage = agent.getActions().stream().filter(a -> shortName(a).equals("triage")).findFirst().orElseThrow();
        assertThat(triage.getInputs()).extracting(AgentStructureTest::typeName).contains("OrderFacts");
        assertThat(agent.getGoals()).hasSize(1);
    }
}
