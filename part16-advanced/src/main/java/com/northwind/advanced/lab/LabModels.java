package com.northwind.advanced.lab;

import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.common.ai.model.PricingModel;
import com.northwind.advanced.lab.ScriptedChatModel.LabReply;
import com.northwind.advanced.lab.ScriptedChatModel.LabRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/**
 * Part XVI's offline model and embedding service. Replies are scripted; prompts, tool calls, parsing and
 * retries are Embabel's own.
 */
@Configuration
@Profile("lab")
public class LabModels {

    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 1);

    @Bean
    public SpringAiLlmService labStandard() {
        var model = new ScriptedChatModel("lab-standard")
                // Chapter 113: activate the matching skill, read its tone guide, then answer
                .rule("activate-skill", r -> !r.hasToolResult() && tool(r, "activate") != null && r.user().contains("refused"),
                        r -> LabReply.callTool(tool(r, "activate"), "{\"name\":\"returns-letter\"}"))
                .rule("read-reference", r -> r.toolResults().contains("references/tone.md")
                                && !r.toolResults().contains("NorthWind tone guide") && tool(r, "readResource") != null,
                        r -> LabReply.callTool(tool(r, "readResource"),
                                "{\"skillName\":\"returns-letter\",\"resourceType\":\"references\",\"fileName\":\"tone.md\"}"))
                .rule("letter", r -> r.toolResults().contains("NorthWind tone guide"),
                        r -> LabReply.text("Dear Sam, thank you for asking to return your Ridgeline tent. The policy "
                                + "allows returns within 30 days of delivery, and yours arrived 41 days ago, so we "
                                + "can't take it back. We would be glad to arrange a repair or store credit instead. "
                                + "NorthWind Support"))
                .rule("gear-with-skill", r -> r.system().contains("Ultralight means"),
                        r -> LabReply.text("The Featherline (0.95 kg) is the one: it is under the 1.5 kg ultralight "
                                + "threshold for a solo tent."))
                .rule("gear", r -> r.user().contains("lightest"),
                        r -> LabReply.text("The Featherline is our lightest tent."))
                // Chapter 119: the migrated crew
                .rule("researcher", r -> r.user().contains("gear researcher"),
                        r -> LabReply.text("{\"points\":[\"Four-season tents have stronger poles and fewer mesh panels\","
                                + "\"Snow skirts keep wind and spindrift out\",\"Expect 1 kg more than a summer tent\"]}"))
                .rule("writer", r -> r.user().contains("NorthWind's writer"),
                        r -> LabReply.text("{\"title\":\"Choosing a winter tent\",\"body\":\"Pick a four-season tent "
                                + "with strong poles and a snow skirt, and budget for about 1 kg extra.\"}"))
                // Chapter 118: replies that go wrong
                .rule("not-json", r -> r.user().contains("PARSE-ME"),
                        r -> LabReply.text("Sure! The answer is that the order is fine."))
                .rule("call-broken-tool", r -> !r.hasToolResult() && tool(r, "orderStatus") != null,
                        r -> LabReply.callTool(tool(r, "orderStatus"), "{\"orderNumber\":\"NW-40001\"}"))
                .rule("report-tool-result", LabRequest::hasToolResult,
                        r -> LabReply.text("The tool said: " + r.toolResults()));
        return new SpringAiLlmService("lab-standard", "NorthWind Lab", model, DefaultOptionsConverter.INSTANCE,
                CUTOFF, List.of(), PricingModel.usdPer1MTokens(1.00, 4.00), false);
    }

    @Bean
    public EmbeddingService labEmbeddings() {
        return new HashingEmbeddingService();
    }

    public static ScriptedChatModel scripted(SpringAiLlmService service) {
        return (ScriptedChatModel) service.getChatModel();
    }

    private static String tool(LabRequest r, String suffix) {
        return r.tools().keySet().stream().filter(t -> t.endsWith(suffix)).findFirst().orElse(null);
    }
}
