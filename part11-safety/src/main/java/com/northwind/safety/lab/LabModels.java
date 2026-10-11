package com.northwind.safety.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/**
 * Part XI's offline model for the {@code lab} profile. Unlike Part X's models, this one is deliberately
 * naive: it follows instructions wherever they appear, including inside customer messages, documents and
 * tool descriptions. That makes every attack in Part XI reproducible, and shows which defences hold even
 * when the model does the wrong thing.
 */
@Configuration
@Profile("lab")
public class LabModels {

    @Bean
    public SpringAiLlmService labAssistant() {
        return new SpringAiLlmService("lab-assistant", "NorthWind Lab", LabScripts.naive(new ScriptedChatModel("lab-assistant")),
                DefaultOptionsConverter.INSTANCE, null, List.of(),
                PricingModel.usdPer1MTokens(1.00, 4.00), false)
                .withKnowledgeCutoffDate(LocalDate.of(2026, 1, 1));
    }

    public static ScriptedChatModel scripted(SpringAiLlmService service) {
        return (ScriptedChatModel) service.getChatModel();
    }
}
