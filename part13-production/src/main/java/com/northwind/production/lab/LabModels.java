package com.northwind.production.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.production.lab.ScriptedChatModel.LabReply;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Part XIII's offline models for the lab profile: a cheap one for triage and a better one for replies.
 * Replies are scripted; everything around them (prompts, parsing, retries, events, traces) is Embabel's.
 */
@Configuration
@Profile("lab")
public class LabModels {

    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 1);
    private static final Pattern MESSAGE = Pattern.compile("(?s)<message>(.*?)</message>");
    private static final Pattern REMEDY = Pattern.compile("Remedy: (\\w+)\\. Amount: ([0-9.]+) GBP");

    @Bean
    public SpringAiLlmService labMini() {
        var model = new ScriptedChatModel("lab-mini")
                .rule("triage", r -> r.mentions("Classify the customer's problem"), r -> LabReply.text(triage(r.user())));
        return new SpringAiLlmService("lab-mini", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                CUTOFF, List.of(), PricingModel.usdPer1MTokens(0.10, 0.40), false);
    }

    @Bean
    public SpringAiLlmService labStandard() {
        var model = new ScriptedChatModel("lab-standard")
                .rule("reply", r -> r.mentions("Write a short, friendly reply"), r -> LabReply.text(reply(r.user())));
        return new SpringAiLlmService("lab-standard", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                CUTOFF, List.of(), PricingModel.usdPer1MTokens(1.00, 4.00), false);
    }

    /** The scripted model behind a lab service, so tests can inspect prompts, add latency or simulate outages. */
    public static ScriptedChatModel scripted(SpringAiLlmService service) {
        return (ScriptedChatModel) service.getChatModel();
    }

    static String triage(String prompt) {
        Matcher m = MESSAGE.matcher(prompt);
        String message = (m.find() ? m.group(1) : prompt).toLowerCase(Locale.ROOT);
        String category = message.matches("(?s).*(damaged|broken|torn|cracked).*") ? "DAMAGED"
                : message.matches("(?s).*(late|not arrived|hasn't arrived|still waiting).*") ? "LATE"
                : message.matches("(?s).*wrong.*") ? "WRONG_ITEM" : "OTHER";
        boolean urgent = message.matches("(?s).*(urgent|asap|tomorrow|this weekend).*");
        return "{\"category\":\"" + category + "\",\"urgent\":" + urgent + "}";
    }

    static String reply(String prompt) {
        Matcher m = REMEDY.matcher(prompt);
        if (!m.find()) {
            return "Thank you for contacting NorthWind. A member of our team will be in touch.";
        }
        return switch (m.group(1)) {
            case "REFUND" -> "Sorry about this. We have refunded " + m.group(2) + " GBP to your original payment method.";
            case "REPLACE" -> "Sorry about the mix-up. A replacement is on its way and you can keep or return the wrong item.";
            default -> "Thank you for telling us. A member of our customer care team will contact you within one working day.";
        };
    }
}
