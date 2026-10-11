package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.AssistantMessageGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.thinking.ThinkingResponse;
import com.embabel.common.core.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chapter 82: whatever reached the model, NorthWind never asks customers for passwords and never sends
 * them to addresses outside its own domain. This output check holds even if an injection succeeded.
 */
@Component
public class CredentialRequestGuardRail implements AssistantMessageGuardRail {

    private static final Pattern ASKS_FOR_SECRET = Pattern.compile("(?i)\\b(password|passcode|PIN|security code)\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@([\\w-]+(\\.[\\w-]+)+)");

    @Override
    public String getName() {
        return "CredentialRequest";
    }

    @Override
    public String getDescription() {
        return "Blocks replies that ask for secrets or point customers to outside email addresses";
    }

    @Override
    public ValidationResult validate(String output, Blackboard blackboard) {
        if (ASKS_FOR_SECRET.matcher(output).find()) {
            return Verdicts.block("ASKS_FOR_SECRET", "Reply asks the customer for a secret");
        }
        Matcher m = EMAIL.matcher(output);
        while (m.find()) {
            if (!m.group(1).equalsIgnoreCase("northwind.example")) {
                return Verdicts.block("OUTSIDE_ADDRESS", "Reply points to an outside address: " + m.group());
            }
        }
        return Verdicts.pass();
    }

    @Override
    public ValidationResult validate(ThinkingResponse<?> response, Blackboard blackboard) {
        return validate(String.valueOf(response.getResult()), blackboard);
    }
}
