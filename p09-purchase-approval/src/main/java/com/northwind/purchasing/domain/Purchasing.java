package com.northwind.purchasing.domain;

import java.math.BigDecimal;
import java.util.List;

/** P9's domain. Records only: every object that may wait in a paused process must be serialisable. */
public final class Purchasing {

    private Purchasing() {
    }

    /** Who is asking. Bound by the application from the authenticated user. */
    public record Requester(String employeeId, String department) {
    }

    /** A structured purchase request, extracted by a model from the employee's free text. */
    public record PurchaseRequest(String supplier, String item, int quantity, BigDecimal unitPrice, String justification) {

        public BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public enum Route { AUTO_APPROVE, NEEDS_APPROVAL, REJECT }

    public record PolicyCheck(Route route, BigDecimal total, String reason) {
    }

    /** A model's briefing for the approver: helpful, but advisory. */
    public record RiskNote(String summary, List<String> concerns) {
    }

    /** The decision. From a person (via WaitFor) or from policy (auto-approval). */
    public record ApprovalDecision(boolean approved, String decidedBy, String comment) {
    }

    public record PurchaseResult(String status, String poNumber, BigDecimal total, String decidedBy, String comment) {
    }
}
