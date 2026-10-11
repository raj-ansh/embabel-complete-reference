package com.northwind.mcpclient.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.mcpclient.lab.ScriptedChatModel.LabReply;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/** P12's offline model for the lab profile: uses whichever stock tool it is offered. */
@Configuration
@Profile("lab")
public class LabModels {

    @Bean
    public SpringAiLlmService labBalanced() {
        var model = new ScriptedChatModel("lab-balanced")
                .rule("call stock tool", r -> !r.hasToolResult() && stockTool(r) != null,
                        r -> LabReply.callTool(stockTool(r), "{\"sku\":\"TENT-2P\"}"))
                .rule("answer", r -> r.hasToolResult(), r -> LabReply.text("Stock check: " + r.toolResults()));
        return new SpringAiLlmService("lab-balanced", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                null, List.of(), PricingModel.usdPer1MTokens(1.00, 4.00), false)
                .withKnowledgeCutoffDate(LocalDate.of(2026, 1, 1));
    }

    private static String stockTool(ScriptedChatModel.LabRequest r) {
        return r.tools().keySet().stream().filter(name -> name.endsWith("stockLevel")).findFirst().orElse(null);
    }
}
