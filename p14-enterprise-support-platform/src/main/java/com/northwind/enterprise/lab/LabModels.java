package com.northwind.enterprise.lab;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.enterprise.lab.ScriptedChatModel.LabReply;
import com.northwind.enterprise.lab.ScriptedChatModel.LabRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P14's offline models: a cheap router that classifies by keywords, and a standard model that uses whichever
 * tools it is offered, then answers from what they returned. Replies are scripted; tool calls are real.
 */
@Configuration
@Profile("lab")
public class LabModels {

    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 1);
    private static final Pattern REQUEST = Pattern.compile("(?s).*<request>\\s*(.*?)\\s*</request>");
    private static final Pattern ORDER = Pattern.compile("NW-\\d{5}");
    private static final Pattern INVOICE = Pattern.compile("INV-\\d{4}");
    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d{1,2})?) (?:pounds|GBP)");

    @Bean
    public SpringAiLlmService labMini() {
        var model = new ScriptedChatModel("lab-mini")
                .rule("route", r -> r.mentions("Route the customer's request"), r -> LabReply.text(route(request(r))));
        return new SpringAiLlmService("lab-mini", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                CUTOFF, List.of(), PricingModel.usdPer1MTokens(0.10, 0.40), false);
    }

    @Bean
    public SpringAiLlmService labStandard() {
        var model = new ScriptedChatModel("lab-standard")
                .rule("refund", r -> !r.hasToolResult() && tool(r, "requestRefund") != null && find(INVOICE, r) != null
                                && lower(r).contains("refund") && find(AMOUNT, r) != null,
                        r -> LabReply.callTool(tool(r, "requestRefund"), "{\"invoiceNumber\":\"" + find(INVOICE, r)
                                + "\",\"amount\":" + amount(r) + ",\"reason\":\"customer request\"}"))
                .rule("invoice", r -> !r.hasToolResult() && tool(r, "invoiceStatus") != null && find(INVOICE, r) != null,
                        r -> LabReply.callTool(tool(r, "invoiceStatus"), "{\"invoiceNumber\":\"" + find(INVOICE, r) + "\"}"))
                .rule("order", r -> !r.hasToolResult() && tool(r, "orderStatus") != null && find(ORDER, r) != null,
                        r -> LabReply.callTool(tool(r, "orderStatus"), "{\"orderNumber\":\"" + find(ORDER, r) + "\"}"))
                .rule("catalogue", r -> !r.hasToolResult() && tool(r, "productsInCategory") != null,
                        r -> LabReply.callTool(tool(r, "productsInCategory"), "{\"category\":\"" + category(r) + "\"}"))
                .rule("policies", r -> !r.hasToolResult() && tool(r, "vectorSearch") != null,
                        r -> LabReply.callTool(tool(r, "vectorSearch"),
                                "{\"query\":\"" + request(r).replaceAll("[\"\\s]+", " ") + "\",\"topK\":2}"))
                .rule("answer", LabRequest::hasToolResult,
                        r -> LabReply.text(r.toolResults().replaceAll("\\s+", " ").strip()));
        return new SpringAiLlmService("lab-standard", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                CUTOFF, List.of(), PricingModel.usdPer1MTokens(1.00, 4.00), false);
    }

    public static ScriptedChatModel scripted(SpringAiLlmService service) {
        return (ScriptedChatModel) service.getChatModel();
    }

    static String route(String request) {
        String text = request.toLowerCase(Locale.ROOT);
        String route = text.matches("(?s).*(invoice|refund|charged|payment).*") ? "BILLING"
                : text.matches("(?s).*(which|compare|recommend|best|lightest).*") ? "RESEARCH"
                : text.matches("(?s).*(order|delivery|return|arriv).*") ? "SUPPORT" : "HUMAN";
        return "{\"route\":\"" + route + "\",\"reason\":\"keywords suggest " + route.toLowerCase(Locale.ROOT) + "\"}";
    }

    private static String request(LabRequest r) {
        Matcher m = REQUEST.matcher(r.user());
        return m.find() ? m.group(1) : r.user();
    }

    private static String lower(LabRequest r) {
        return request(r).toLowerCase(Locale.ROOT);
    }

    private static String find(Pattern pattern, LabRequest r) {
        Matcher m = pattern.matcher(request(r));
        return m.find() ? m.group() : null;
    }

    private static String amount(LabRequest r) {
        Matcher m = AMOUNT.matcher(request(r));
        return m.find() ? m.group(1) : "0";
    }

    private static String category(LabRequest r) {
        String text = lower(r);
        return text.contains("stove") ? "stove" : text.contains("bag") ? "bag" : "tent";
    }

    private static String tool(LabRequest r, String suffix) {
        return r.tools().keySet().stream().filter(t -> t.endsWith(suffix)).findFirst().orElse(null);
    }
}
