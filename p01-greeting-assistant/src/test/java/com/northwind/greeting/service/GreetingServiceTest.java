package com.northwind.greeting.service;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.northwind.greeting.customer.Customer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests: no Spring, no model. FakeOperationContext supplies an Ai whose responses we script. */
class GreetingServiceTest {

    private final GreetingProperties properties = new GreetingProperties("warm and concise", 40, 0.7);

    private final Customer mateo = new Customer("C-1002", "Mateo", Customer.LoyaltyTier.STANDARD, "es",
            "Trailblazer Waterproof Boot", LocalDate.of(2026, 9, 30));

    @Test
    void promptContainsOnlyTrustedFactsAndTheRules() {
        String prompt = new GreetingService(FakeOperationContext.create().ai(), properties)
                .prompt(mateo, Occasion.ORDER_SHIPPED);

        assertThat(prompt)
                .contains("Mateo", "Trailblazer Waterproof Boot", "\"es\"", "at most 40 words")
                .contains(Occasion.ORDER_SHIPPED.instruction())
                .contains("do not invent");
    }

    @Test
    void returnsTheModelTextAndUsesTheConfiguredTemperature() {
        var context = FakeOperationContext.create();
        context.expectResponse("¡Hola Mateo! Tus botas ya van en camino.");

        String greeting = new GreetingService(context.ai(), properties).greet(mateo, Occasion.ORDER_SHIPPED);

        assertThat(greeting).startsWith("¡Hola Mateo!");
        var invocation = ((FakePromptRunner) context.promptRunner()).getLlmInvocations().getFirst();
        assertThat(invocation.getInteraction().getLlm().getTemperature()).isEqualTo(0.7);
    }
}
