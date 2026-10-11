package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chapter 81: a global guardrail, registered by class name in configuration and applied to every model
 * call. Global guardrails are plain objects created by Embabel, not Spring beans: no constructor arguments.
 */
public class AuditInputGuardRail implements UserInputGuardRail {

    public static final AtomicInteger CALLS = new AtomicInteger();

    @Override
    public String getName() {
        return "AuditInput";
    }

    @Override
    public String getDescription() {
        return "Records that a prompt was checked (length only, never content)";
    }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        CALLS.incrementAndGet();
        return Verdicts.flag("AUDIT", "Prompt of " + input.length() + " characters checked", ValidationSeverity.INFO);
    }
}
