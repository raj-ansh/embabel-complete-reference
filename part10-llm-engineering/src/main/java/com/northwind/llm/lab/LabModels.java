package com.northwind.llm.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.llm.byok.UserModelFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/**
 * Three offline "lab" models for the {@code lab} profile, registered exactly as a custom provider
 * would register real ones (Chapter 73). Prices are invented for teaching and are per million tokens.
 */
@Configuration
@Profile("lab")
public class LabModels {

    public static final String PROVIDER = "NorthWind Lab";
    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 1);

    @Bean
    public SpringAiLlmService labMini() {
        return new SpringAiLlmService("lab-mini", PROVIDER, LabScripts.mini(new ScriptedChatModel("lab-mini")),
                DefaultOptionsConverter.INSTANCE, null, List.of(),
                PricingModel.usdPer1MTokens(0.10, 0.40), false)
                .withKnowledgeCutoffDate(CUTOFF);               // also adds a knowledge-cutoff prompt contributor
    }

    @Bean
    public SpringAiLlmService labStandard() {
        return new SpringAiLlmService("lab-standard", PROVIDER, LabScripts.standard(new ScriptedChatModel("lab-standard")),
                DefaultOptionsConverter.INSTANCE, CUTOFF, List.of(),
                PricingModel.usdPer1MTokens(1.00, 4.00), false);   // cutoff via constructor only: no prompt line (Chapter 70)
    }

    @Bean
    public SpringAiLlmService labReasoner() {
        return new SpringAiLlmService("lab-reasoner", PROVIDER, LabScripts.reasoner(new ScriptedChatModel("lab-reasoner")),
                DefaultOptionsConverter.INSTANCE, CUTOFF, List.of(),
                PricingModel.usdPer1MTokens(5.00, 25.00), true);   // supports reasoning ("thinking") output
    }

    /**
     * Chapter 73: a "model built from a user's key". The lab version needs no network; the user pays,
     * so NorthWind's cost tracking sees it as free ({@code ALL_YOU_CAN_EAT}).
     */
    @Bean
    public UserModelFactory labUserModelFactory() {
        return credential -> new SpringAiLlmService(
                "byok-" + credential.getProvider().toLowerCase() + "-" + last4(credential.getApiKey()), credential.getProvider(),
                LabScripts.standard(new ScriptedChatModel("byok")), DefaultOptionsConverter.INSTANCE, CUTOFF, List.of(),
                PricingModel.getALL_YOU_CAN_EAT(), false);
    }

    private static String last4(String key) {
        return key.substring(Math.max(0, key.length() - 4));
    }

    /** The scripted model behind a lab service, so tests can inspect what Embabel sent. */
    public static ScriptedChatModel scripted(SpringAiLlmService service) {
        return (ScriptedChatModel) service.getChatModel();
    }
}
