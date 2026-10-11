package com.northwind.safety.injection;

import com.embabel.agent.api.tool.callback.AfterToolResultContext;
import com.embabel.agent.api.tool.callback.ToolLoopTransformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Chapter 82: tool results are data written by someone else (customers, suppliers, web pages). Text that
 * addresses an AI is removed, and the rest is marked as untrusted before the model sees it.
 * This lowers the risk; it cannot remove it. The decisive controls are in code (RefundTools) and on the
 * output (CredentialRequestGuardRail).
 */
@Component
public class UntrustedContentTransformer implements ToolLoopTransformer {

    private static final List<Pattern> ADDRESSES_AN_AI = List.of(
            Pattern.compile("(?i)\\b(AI|assistant|chatbot|language model)\\s*(assistant)?\\s*:.*"),
            Pattern.compile("(?i)ignore (all |any )?(previous|prior|above) instructions.*"),
            Pattern.compile("(?i)\\bsystem prompt\\b.*"));

    @Override
    public String transformAfterToolResult(AfterToolResultContext context) {
        String text = context.getResultAsString();
        for (Pattern p : ADDRESSES_AN_AI) {
            text = p.matcher(text).replaceAll("[text addressed to an AI removed]");
        }
        return "<untrusted-data source=\"" + context.getToolCall().getName() + "\">\n" + text + "\n</untrusted-data>";
    }
}
