package com.northwind.returns.domain;

import java.math.BigDecimal;

/** The outcome of applying NorthWind's returns policy. Produced by code, never by a model. */
public record ReturnDecision(Outcome outcome, BigDecimal refundAmount, boolean freeReturnLabel, String reason) {

    public enum Outcome { APPROVED, REJECTED }

    public static ReturnDecision approved(BigDecimal amount, boolean freeLabel, String reason) {
        return new ReturnDecision(Outcome.APPROVED, amount, freeLabel, reason);
    }

    public static ReturnDecision rejected(String reason) {
        return new ReturnDecision(Outcome.REJECTED, BigDecimal.ZERO, false, reason);
    }
}
