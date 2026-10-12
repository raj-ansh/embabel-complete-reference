package com.northwind.production.domain;

import java.math.BigDecimal;

/** Part XIII: the types that flow through the issue-resolution agent. */
public final class Issues {

    private Issues() {
    }

    /** What the customer reported. The customer id comes from authentication, never from the message. */
    public record IssueReport(String customerId, String orderNumber, String message) {
    }

    public enum Category { DAMAGED, LATE, WRONG_ITEM, OTHER }

    /** The model's reading of the message. */
    public record Triage(Category category, boolean urgent) {
    }

    /** Facts from the order system. */
    public record OrderFacts(String orderNumber, String customerId, String status, BigDecimal value) {
    }

    public enum Remedy { REFUND, REPLACE, ESCALATE }

    /** What NorthWind will do, decided by code. */
    public record Resolution(Remedy remedy, BigDecimal amount, String reason) {
    }

    /** The answer to the customer. */
    public record CustomerReply(String text, Remedy remedy) {
    }
}
