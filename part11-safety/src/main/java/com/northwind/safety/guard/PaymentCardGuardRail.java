package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chapter 81: card numbers must never reach a model provider. Candidate numbers are confirmed with the
 * Luhn checksum, so ordinary long numbers (order references, phone numbers) are not blocked.
 */
@Component
public class PaymentCardGuardRail implements UserInputGuardRail {

    private static final Pattern CANDIDATE = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    @Override
    public String getName() {
        return "PaymentCard";
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
                return Verdicts.block("CARD_NUMBER", "Input contains a payment card number");
            }
        }
        return Verdicts.pass();
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
