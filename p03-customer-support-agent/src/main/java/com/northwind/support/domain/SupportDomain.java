package com.northwind.support.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The support agent's vocabulary. Each record is a distinct type the planner can reason about.
 * Grouped in one file for the book; in a real project each would be its own file.
 */
public final class SupportDomain {

    private SupportDomain() {
    }

    /** Who is asking. Supplied by the authenticated caller, never extracted from the message. */
    public record CustomerContext(@NotBlank String customerId) {
    }

    /** What the customer is asking, understood by a model. */
    public record SupportRequest(
            @NotNull Category category,
            @Pattern(regexp = "NW-\\d{4,6}") String orderNumber,     // optional
            String productSku,                                       // optional
            @NotBlank @Size(max = 200) String summary,
            @NotNull Urgency urgency) {

        public enum Category { ORDER_STATUS, PRODUCT_QUESTION, BILLING, RETURN, COMPLAINT, OTHER }

        public enum Urgency { LOW, NORMAL, HIGH }
    }

    /** What NorthWind knows about the customer. */
    public record CustomerProfile(String customerId, String firstName, Tier tier, int openTickets) {

        public enum Tier { STANDARD, SILVER, GOLD }
    }

    /** An order as seen by support. */
    public record OrderSummary(String number, String customerId, String status, LocalDate expectedDelivery,
                               BigDecimal total) {
    }

    /** The facts gathered and the routing decision, produced by code. */
    public record IssueAssessment(
            SupportRequest.Category category,
            List<String> facts,
            boolean escalate,
            String escalationReason) {
    }

    /** The agent's goal: what goes back to the customer. */
    public record SupportResponse(
            @NotBlank @Size(max = 1500) String message,
            boolean escalatedToHuman,
            String ticketReference) {
    }
}
