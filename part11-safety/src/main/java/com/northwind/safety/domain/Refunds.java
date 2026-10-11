package com.northwind.safety.domain;

import java.math.BigDecimal;

/** Chapter 81: a refund the model proposes, and what NorthWind's policy does with it. */
public final class Refunds {

    private Refunds() {
    }

    public record RefundProposal(String orderNumber, BigDecimal amount, String reason) {
    }

    public enum Route { AUTO_APPROVED, NEEDS_SUPERVISOR, REJECTED }

    public record RefundOutcome(RefundProposal proposal, Route route, String note) {
    }
}
