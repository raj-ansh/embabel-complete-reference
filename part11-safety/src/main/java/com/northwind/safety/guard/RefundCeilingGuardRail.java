package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.AssistantMessageGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.thinking.ThinkingResponse;
import com.embabel.common.core.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chapter 81: a backstop. The refund policy in code decides approvals; this guardrail rejects any model
 * output proposing more than the absolute ceiling, which no approval path may pay.
 */
@Component
public class RefundCeilingGuardRail implements AssistantMessageGuardRail {

    public static final BigDecimal CEILING = new BigDecimal("1000");
    private static final Pattern AMOUNT = Pattern.compile("\"amount\"\\s*:\\s*([0-9.]+)");

    @Override
    public String getName() {
        return "RefundCeiling";
    }

    @Override
    public String getDescription() {
        return "Blocks refund proposals above the absolute ceiling";
    }

    @Override
    public ValidationResult validate(String output, Blackboard blackboard) {
        Matcher m = AMOUNT.matcher(output);
        if (m.find() && new BigDecimal(m.group(1)).compareTo(CEILING) > 0) {
            return Verdicts.block("REFUND_CEILING", "Proposed refund £" + m.group(1) + " exceeds the £" + CEILING + " ceiling");
        }
        return Verdicts.pass();
    }

    @Override
    public ValidationResult validate(ThinkingResponse<?> response, Blackboard blackboard) {
        return validate(String.valueOf(response.getResult()), blackboard);
    }
}
