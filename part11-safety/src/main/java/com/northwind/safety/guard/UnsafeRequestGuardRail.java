package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Chapter 81: blocks requests for help with dangerous misuse of NorthWind's products before any model
 * sees them. Deterministic rules first: cheap, explainable and testable.
 */
@Component
public class UnsafeRequestGuardRail implements UserInputGuardRail {

    private static final List<Pattern> UNSAFE = List.of(
            Pattern.compile("(?i)refill\\w*\\s+(a\\s+)?(disposable|single-use)\\s+(gas\\s+)?canister"),
            Pattern.compile("(?i)(bypass|remove|disable)\\w*\\s+(the\\s+)?(regulator|safety valve)"),
            Pattern.compile("(?i)(use|run|light)\\w*\\s+(the\\s+|a\\s+)?stove\\s+inside\\s+(the\\s+|a\\s+|my\\s+)?tent"));

    @Override
    public String getName() {
        return "UnsafeRequest";
    }

    @Override
    public String getDescription() {
        return "Blocks requests for help with dangerous misuse of gas equipment";
    }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        return UNSAFE.stream().anyMatch(p -> p.matcher(input).find())
                ? Verdicts.block("UNSAFE_REQUEST", "Request asks for help with dangerous misuse of gas equipment")
                : Verdicts.pass();
    }
}
