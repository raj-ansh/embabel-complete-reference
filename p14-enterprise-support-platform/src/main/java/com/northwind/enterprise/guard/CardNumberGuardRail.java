package com.northwind.enterprise.guard;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationError;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P14 (Chapter 81's guardrail, platform-wide): card numbers must never reach a model provider. Registered by
 * class name in application.yml, so it runs before every model call of every agent, the router included.
 */
public class CardNumberGuardRail implements UserInputGuardRail {

    private static final Pattern CANDIDATE = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    @Override
    public String getName() {
        return "CardNumber";
    }

    @Override
    public String getDescription() {
        return "Blocks input that contains a payment card number";
    }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        Matcher m = CANDIDATE.matcher(input);
        while (m.find()) {
            if (luhn(m.group().replaceAll("[ -]", ""))) {
                return new ValidationResult(false, List.of(new ValidationError(
                        "CARD_NUMBER", "Input contains a payment card number", ValidationSeverity.CRITICAL)));
            }
        }
        return new ValidationResult(true, List.of());
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl && (d *= 2) > 9) {
                d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
