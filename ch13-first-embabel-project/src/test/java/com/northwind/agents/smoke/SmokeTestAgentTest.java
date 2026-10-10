package com.northwind.agents.smoke;

import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test: call the action directly with a fake context. No Spring, no model. */
class SmokeTestAgentTest {

    @Test
    void greetsThePersonInTheMessage() {
        var context = FakeOperationContext.create();
        context.expectResponse(new Greeting("Asha", "Welcome to NorthWind Outfitters, Asha!"));

        Greeting greeting = new SmokeTestAgent().greet(new UserInput("Hi, I'm Asha"), context);

        assertThat(greeting.recipient()).isEqualTo("Asha");
        var prompt = ((FakePromptRunner) context.promptRunner()).getLlmInvocations().getFirst().getPrompt();
        assertThat(prompt).contains("Hi, I'm Asha").contains("<message>");
    }
}
