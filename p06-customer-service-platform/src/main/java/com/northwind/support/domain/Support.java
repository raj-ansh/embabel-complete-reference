package com.northwind.support.domain;

import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;

/** P6's domain: the supervisor's view of a message, each specialist's contract, and the reply. */
public final class Support {

    private Support() {
    }

    public enum Tier { STANDARD, GOLD }

    /** The signed-in customer, bound from authentication. Specialists read it from the blackboard. */
    public record Customer(String customerId, Tier tier) {
    }

    public enum Intent { BILLING, ORDER, TECHNICAL, OTHER }

    public enum BillingKind { INVOICE, REFUND }

    /** The supervisor's typed reading of a message. Fields that don't apply are null. */
    public record Triage(Intent intent,
                         @Nullable String orderNumber,
                         @Nullable BillingKind billingKind,
                         @Nullable BigDecimal amount,
                         @Nullable String question,
                         String summary) {
    }

    // --- specialist contracts: input in, answer out -------------------------------------------

    public record BillingQuestion(String orderNumber, BillingKind kind, @Nullable BigDecimal amount) {
    }

    /** A refund billing may not issue on its own. Approval happens in the TOP-LEVEL process (see P6 chapter). */
    public record RefundProposal(String orderNumber, BigDecimal amount, String reason) {
    }

    /** Billing's answer: either settled facts, or a proposal that needs a support lead. */
    public record BillingAnswer(String facts, @Nullable RefundProposal proposal) {
    }

    /** A support lead's decision, supplied by a person through WaitFor. */
    public record RefundApproval(boolean approved, String decidedBy, String note) {
    }

    public record OrderQuestion(String orderNumber) {
    }

    public record OrderAnswer(String facts) {
    }

    public record TechnicalQuestion(String question) {
    }

    public record TechnicalAnswer(String facts, List<String> guidesUsed) {
    }

    // --- convergence --------------------------------------------------------------------------

    /** What a specialist established. The reply is written only from these facts. */
    public record SpecialistResult(String specialist, String facts) {
    }

    public record CustomerReply(String text, String specialist) {
    }
}
