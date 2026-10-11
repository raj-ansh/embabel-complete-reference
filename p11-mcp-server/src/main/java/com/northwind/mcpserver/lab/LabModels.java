package com.northwind.mcpserver.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.mcpserver.lab.ScriptedChatModel.LabReply;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/** P11's offline model for the lab profile: checks stock with a tool, then recommends. */
@Configuration
@Profile("lab")
public class LabModels {

    @Bean
    public SpringAiLlmService labBalanced() {
        var model = new ScriptedChatModel("lab-balanced")
                .rule("advice: check stock", r -> r.mentions("Recommend NorthWind products") && !r.hasToolResult(),
                        r -> LabReply.callTool("stockLevel", "{\"sku\":\"TENT-2P\"}"))
                .rule("advice: answer", r -> r.mentions("Recommend NorthWind products") && r.hasToolResult(),
                        r -> LabReply.text("{\"skus\":[\"TENT-2P\",\"BAG-15\"],\"reason\":\"Light two-person shelter in stock ("
                                + r.toolResults().replace("\"", "") + ") and a bag rated for mild nights\"}"));
        return new SpringAiLlmService("lab-balanced", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                null, List.of(), PricingModel.usdPer1MTokens(1.00, 4.00), false)
                .withKnowledgeCutoffDate(LocalDate.of(2026, 1, 1));
    }
}
