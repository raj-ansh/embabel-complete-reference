package com.northwind.mcpserver.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.mcpserver.lab.ScriptedChatModel.LabReply;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** P11's offline model for the lab profile: checks stock with a tool, then recommends. */
@Configuration
@Profile("lab")
public class LabModels {

    @Bean
    public SpringAiLlmService labBalanced() {
        var model = new ScriptedChatModel("lab-balanced")
                // Chapter 89: A2A requests arrive as text; Embabel asks the model which goal they are for
                .rule("rank goals", r -> r.mentions("Your job is rank objects"),
                        r -> LabReply.text("{\"rankings\":[{\"name\":\"" + bestChoice(r.system() + "\n" + r.user()) + "\",\"confidence\":0.92}]}"))
                .rule("advice: check stock", r -> r.mentions("Recommend NorthWind products") && !r.hasToolResult(),
                        r -> LabReply.callTool("stockLevel", "{\"sku\":\"TENT-2P\"}"))
                .rule("advice: answer", r -> r.mentions("Recommend NorthWind products") && r.hasToolResult(),
                        r -> LabReply.text("{\"skus\":[\"TENT-2P\",\"BAG-15\"],\"reason\":\"Light two-person shelter in stock ("
                                + r.toolResults().replace("\"", "") + ") and a bag rated for mild nights\"}"));
        return new SpringAiLlmService("lab-balanced", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                null, List.of(), PricingModel.usdPer1MTokens(1.00, 4.00), false)
                .withKnowledgeCutoffDate(LocalDate.of(2026, 1, 1));
    }

    /** Picks the "- name: description" choice sharing most words with the user input, as a model might. */
    static String bestChoice(String prompt) {
        var input = Set.copyOf(Arrays.asList(prompt.replaceAll("(?s).*User input: <(.*?)>.*", "$1").toLowerCase().split("\\W+")));
        return prompt.lines().map(String::strip)
                .filter(line -> line.startsWith("- ") && line.contains(": "))
                .max(Comparator.comparingLong(line -> Arrays.stream(line.toLowerCase().split("\\W+"))
                        .filter(word -> word.length() > 3 && input.contains(word)).count()))
                .map(line -> line.substring(2, line.indexOf(": ")))
                .orElse("none");
    }
}
