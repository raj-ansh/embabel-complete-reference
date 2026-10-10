package com.northwind.springai.triage;

/** The structured result we want from the model for each support ticket. */
public record TicketTriage(Category category, int urgency, String summary) {

    public enum Category { BILLING, SHIPPING, PRODUCT, ACCOUNT, OTHER }
}
