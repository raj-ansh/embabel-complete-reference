package com.northwind.agents.smoke;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: starts the Spring Boot application with the real AgentPlatform,
 * with model calls replaced by Mockito stubs.
 */
class SmokeTestAgentIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Test
    void platformDeploysTheAgentAndReachesTheGoal() {
        whenCreateObject(prompt -> prompt.contains("welcome to NorthWind"), Greeting.class)
                .thenReturn(new Greeting("Asha", "Welcome to NorthWind Outfitters, Asha!"));

        Greeting greeting = AgentInvocation.create(agentPlatform, Greeting.class)
                .invoke(new UserInput("Hi, I'm Asha"));

        assertThat(greeting.message()).contains("Asha");
        assertThat(agentPlatform.agents()).anyMatch(a -> a.getName().equals("SmokeTestAgent"));
    }
}
