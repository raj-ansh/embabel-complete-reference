package com.northwind.multiagent.returns;

import java.math.BigDecimal;

/** The returns domain shared by the returns desk and its eligibility subagent. */
public final class Returns {

    private Returns() {
    }

    public enum Tier { STANDARD, GOLD }

    /** Who is asking. Bound by the application, never produced by a model. */
    public record Customer(String customerId, Tier tier) {
    }

    public record ReturnRequest(String orderNumber, String sku, String reason, int daysSinceDelivery) {
    }

    public record OrderFacts(String orderNumber, String sku, boolean finalSale, BigDecimal paid, boolean found) {

        public static OrderFacts notFound(String orderNumber) {
            return new OrderFacts(orderNumber, null, false, BigDecimal.ZERO, false);
        }
    }

    public record EligibilityDecision(boolean eligible, String rule, BigDecimal refundAmount) {
    }

    public record ReturnOutcome(String orderNumber, String status, String message) {
    }
}
