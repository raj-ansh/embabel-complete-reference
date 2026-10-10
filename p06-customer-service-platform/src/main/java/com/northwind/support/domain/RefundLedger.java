package com.northwind.support.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A stand-in for the payment system. Idempotent per order: a second refund for the same order is ignored. */
@Component
public class RefundLedger {

    public record Refund(String orderNumber, BigDecimal amount, String approvedBy) {
    }

    private final Map<String, Refund> refunds = new ConcurrentHashMap<>();

    /** Returns true if this call issued the refund, false if one already existed. */
    public boolean issue(String orderNumber, BigDecimal amount, String approvedBy) {
        return refunds.putIfAbsent(orderNumber, new Refund(orderNumber, amount, approvedBy)) == null;
    }

    public List<Refund> issued() {
        return List.copyOf(refunds.values());
    }

    public void clear() {
        refunds.clear();
    }
}
