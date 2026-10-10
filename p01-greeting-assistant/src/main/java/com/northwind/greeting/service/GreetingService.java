package com.northwind.greeting.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.greeting.customer.Customer;
import org.springframework.stereotype.Service;

/**
 * "A little AI": an ordinary Spring service that uses Embabel's Ai for one model call.
 * No agent, no planning: just a well-built prompt from trusted data, and a result.
 */
@Service
public class GreetingService {

    private final Ai ai;
    private final GreetingProperties properties;

    public GreetingService(Ai ai, GreetingProperties properties) {
        this.ai = ai;
        this.properties = properties;
    }

    public String greet(Customer customer, Occasion occasion) {
        return ai
                .withLlm(LlmOptions.withDefaultLlm().withTemperature(properties.temperature()))
                .generateText(prompt(customer, occasion));
    }

    /** Package-private so the prompt itself can be unit-tested. */
    String prompt(Customer customer, Occasion occasion) {
        return """
                You write short customer greetings for NorthWind Outfitters, an outdoor-gear retailer.
                Brand voice: %s.

                Task: %s
                Write in the language with ISO code "%s".
                Use at most %d words. Address the customer by first name.
                Output only the greeting text: no subject line, no signature, no placeholders.

                Facts you may use (do not invent others):
                - First name: %s
                - Loyalty tier: %s
                - Most recent purchase: %s
                - Member since: %d
                """.formatted(
                properties.brandVoice(),
                occasion.instruction(),
                customer.preferredLanguage(),
                properties.maxWords(),
                customer.firstName(),
                customer.tier(),
                customer.lastPurchase(),
                customer.memberSince().getYear());
    }
}
