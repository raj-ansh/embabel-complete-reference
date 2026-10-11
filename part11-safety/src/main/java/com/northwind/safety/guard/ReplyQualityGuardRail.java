package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.AssistantMessageGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.thinking.ThinkingResponse;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Chapter 81: checks what the model wrote before anyone sees it. Leftover template placeholders and a
 * leaked canary token are blocking; an over-long reply is only reported.
 */
@Component
public class ReplyQualityGuardRail implements AssistantMessageGuardRail {

    /** A unique marker placed in system prompts: if it ever appears in output, the prompt has leaked. */
    public static final String CANARY = "NW-CANARY-7731";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\[(Name|Customer|Order number|Date)]");
    private static final int MAX_WORDS = 120;

    @Override
    public String getName() {
        return "ReplyQuality";
    }

    @Override
    public String getDescription() {
        return "Blocks replies with placeholders or leaked system prompts; reports over-long replies";
    }

    @Override
    public ValidationResult validate(String output, Blackboard blackboard) {
        if (output.contains(CANARY)) {
            return Verdicts.block("PROMPT_LEAK", "Reply contains the system prompt canary");
        }
        if (PLACEHOLDER.matcher(output).find()) {
            return Verdicts.block("PLACEHOLDER", "Reply contains an unfilled placeholder");
        }
        int words = output.isBlank() ? 0 : output.trim().split("\\s+").length;
        if (words > MAX_WORDS) {
            return Verdicts.flag("TOO_LONG", "Reply has " + words + " words", ValidationSeverity.WARNING);
        }
        return Verdicts.pass();
    }

    @Override
    public ValidationResult validate(ThinkingResponse<?> response, Blackboard blackboard) {
        return validate(String.valueOf(response.getResult()), blackboard);
    }
}
