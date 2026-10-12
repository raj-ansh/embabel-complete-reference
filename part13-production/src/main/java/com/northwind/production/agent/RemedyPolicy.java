package com.northwind.production.agent;

import com.northwind.production.domain.Issues.OrderFacts;
import com.northwind.production.domain.Issues.Remedy;
import com.northwind.production.domain.Issues.Resolution;
import com.northwind.production.domain.Issues.Triage;

import java.math.BigDecimal;

/**
 * Part XIII: NorthWind's remedy rules. Plain code, no model: money and promises are decided here, where
 * they can be read, reviewed and unit tested (Chapter 90).
 */
public final class RemedyPolicy {

    /** Refunds above this value need a person. */
    public static final BigDecimal AUTO_REFUND_LIMIT = new BigDecimal("150.00");

    private RemedyPolicy() {
    }

    public static Resolution decide(Triage triage, OrderFacts order) {
        return switch (triage.category()) {
            case DAMAGED -> order.value().compareTo(AUTO_REFUND_LIMIT) <= 0
                    ? new Resolution(Remedy.REFUND, order.value(), "damaged item within the automatic refund limit")
                    : new Resolution(Remedy.ESCALATE, BigDecimal.ZERO, "damaged item above the automatic refund limit");
            case WRONG_ITEM -> new Resolution(Remedy.REPLACE, BigDecimal.ZERO, "wrong item sent");
            case LATE -> "DELIVERED".equals(order.status())
                    ? new Resolution(Remedy.ESCALATE, BigDecimal.ZERO, "reported late but recorded as delivered")
                    : new Resolution(Remedy.REFUND, new BigDecimal("4.99"), "late delivery: shipping refunded");
            case OTHER -> new Resolution(Remedy.ESCALATE, BigDecimal.ZERO, "needs a person");
        };
    }
}
