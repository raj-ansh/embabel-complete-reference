package com.northwind.core.agents;

import com.embabel.agent.api.common.workflow.control.SimpleAgentBuilder;
import com.embabel.agent.core.Agent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chapter 22: an agent defined with the DSL instead of annotations.
 * Any @Bean of type Agent is deployed to the platform, just like an @Agent class.
 */
@Configuration
public class SloganAgentConfiguration {

    public record Slogan(String text) {
    }

    @Bean
    public Agent sloganAgent() {
        return SimpleAgentBuilder
                .returning(Slogan.class)
                .running(context -> context.ai()
                        .withLlmByRole("cheap")
                        .createObject("Write a five-word slogan for NorthWind Outfitters' autumn sale.",
                                Slogan.class))
                .buildAgent("SloganAgent", "Writes a short slogan for a NorthWind campaign");
    }
}
