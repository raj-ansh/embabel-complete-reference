package com.northwind.tenancy.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.tenancy.lab.ScriptedChatModel.LabReply;
import com.northwind.tenancy.lab.ScriptedChatModel.LabRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P13's offline models: one per plan (Chapter 106). Each behaves like a model with tools: it looks up an
 * order if the question names one, otherwise it searches the knowledge it was given, then answers from
 * what the tools returned. It can only use the tools the application offers it.
 */
@Configuration
@Profile("lab")
public class LabModels {

    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 1);
    private static final Pattern QUESTION = Pattern.compile("(?s).*<question>\\s*(.*?)\\s*</question>");   // the last tag pair
    private static final Pattern ORDER = Pattern.compile("NW-\\d{5}");

    @Bean
    public SpringAiLlmService labBasic() {
        return service("lab-basic", PricingModel.usdPer1MTokens(0.10, 0.40));
    }

    @Bean
    public SpringAiLlmService labPremium() {
        return service("lab-premium", PricingModel.usdPer1MTokens(2.00, 8.00));
    }

    public static ScriptedChatModel scripted(SpringAiLlmService service) {
        return (ScriptedChatModel) service.getChatModel();
    }

    private static SpringAiLlmService service(String name, PricingModel pricing) {
        var model = new ScriptedChatModel(name)
                .rule("look up order", r -> !r.hasToolResult() && order(r) != null && tool(r, "orderStatus") != null,
                        r -> LabReply.callTool(tool(r, "orderStatus"), "{\"orderNumber\":\"" + order(r) + "\"}"))
                .rule("search knowledge", r -> !r.hasToolResult() && tool(r, "vectorSearch") != null,
                        r -> LabReply.callTool(tool(r, "vectorSearch"),
                                "{\"query\":\"" + question(r).replaceAll("[\"\\s]+", " ") + "\",\"topK\":2}"))
                .rule("answer", LabRequest::hasToolResult,
                        r -> LabReply.text("[" + name + "] " + r.toolResults().replaceAll("\\s+", " ").strip()));
        return new SpringAiLlmService(name, "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                CUTOFF, List.of(), pricing, false);
    }

    private static String question(LabRequest r) {
        Matcher m = QUESTION.matcher(r.user());
        return m.find() ? m.group(1) : r.user();
    }

    private static String order(LabRequest r) {
        Matcher m = ORDER.matcher(question(r));
        return m.find() ? m.group() : null;
    }

    private static String tool(LabRequest r, String suffix) {
        return r.tools().keySet().stream().filter(t -> t.endsWith(suffix)).findFirst().orElse(null);
    }
}
