package com.northwind.advanced.tooling;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 114: a small developer tool written with Embabel's own metadata. It prints every deployed agent's
 * actions with what they need and produce, and its goals, so a reviewer can see the shape of an agent without
 * running it.
 */
@SpringBootTest
@ActiveProfiles("lab")
class AgentCatalogueTest {

    @Autowired AgentPlatform platform;

    private static String type(String name) {
        return name.replaceAll("^.*[.$]", "");
    }

    @Test
    void catalogue() {
        for (Agent agent : platform.agents().stream().sorted(Comparator.comparing(Agent::getName)).toList()) {
            System.out.println(agent.getName() + ": " + agent.getDescription());
            agent.getActions().forEach(action -> System.out.println("  action " + type(action.getName()) + "("
                    + String.join(", ", action.getInputs().stream().map(i -> type(i.getValue())).sorted().toList())
                    + ") -> " + String.join(", ", action.getOutputs().stream().map(o -> type(o.getValue())).toList())));
            agent.getGoals().forEach(goal -> System.out.println("  goal   " + type(goal.getName()) + " needs "
                    + goal.getInputs().stream().map(i -> type(i.getValue())).toList()));
        }
        assertThat(platform.agents()).hasSizeGreaterThanOrEqualTo(2);
    }
}
