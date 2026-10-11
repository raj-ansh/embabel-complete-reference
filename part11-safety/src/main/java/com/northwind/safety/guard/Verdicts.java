package com.northwind.safety.guard;

import com.embabel.common.core.validation.ValidationError;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;

import java.util.List;

/** Small helpers so that every guardrail reports its findings the same way. */
public final class Verdicts {

    private Verdicts() {
    }

    public static ValidationResult pass() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult block(String code, String message) {
        return new ValidationResult(false, List.of(new ValidationError(code, message, ValidationSeverity.CRITICAL)));
    }

    public static ValidationResult flag(String code, String message, ValidationSeverity severity) {
        return new ValidationResult(severity != ValidationSeverity.ERROR,
                List.of(new ValidationError(code, message, severity)));
    }
}
