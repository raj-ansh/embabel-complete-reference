package com.northwind.llm.domain;

import java.util.List;

/** The support domain used throughout Part X. */
public final class Support {

    private Support() {
    }

    public record Ticket(String id, String customerId, String message) {
    }

    public enum Category { ORDER, RETURN, PRODUCT, COMPLAINT }

    /** The cheap, fast first step: what is this ticket about, and is it risky? */
    public record Triage(Category category, boolean legalRisk, String reason) {
    }

    public record Reply(String subject, String body) {
    }

    public record ProductSummary(String sku, String name, String oneLiner, List<String> highlights) {
    }
}
