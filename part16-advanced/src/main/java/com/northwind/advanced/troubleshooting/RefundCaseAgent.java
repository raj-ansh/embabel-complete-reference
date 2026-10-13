package com.northwind.advanced.troubleshooting;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;

/**
 * Chapter 118: an agent that gets stuck. Deciding a refund needs a CustomerRecord, and no action produces one,
 * so a process started with only a RefundCase has no plan.
 */
@Agent(description = "Decides refund cases")
public class RefundCaseAgent {

    public record RefundCase(String orderNumber, double amount) {
    }

    public record CustomerRecord(String customerId, int ordersLastYear) {
    }

    public record RefundDecision(boolean approved, String reason) {
    }

    @AchievesGoal(description = "Decide a refund case")
    @Action
    public RefundDecision decide(RefundCase refundCase, CustomerRecord customer) {
        boolean approved = refundCase.amount() <= 50 || customer.ordersLastYear() > 5;
        return new RefundDecision(approved, approved ? "within policy" : "needs review");
    }
}
