package com.northwind.safety.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.ActionRetryPolicy;
import com.embabel.agent.mcpserver.security.SecureAgentTool;
import com.northwind.safety.domain.Refunds.RefundProposal;

/**
 * Chapter 84: approving a refund requires the SUPERVISOR role, checked by Spring Security against the
 * caller's authentication when the action runs, wherever the request came from.
 */
@Agent(description = "Approve a large refund proposed by the support assistant")
public class RefundApprovalAgent {

    public record ApprovalRequest(String orderNumber, String amount) {
    }

    public record ApprovedRefund(String orderNumber, String amount, String approvedBy) {
    }

    @Action(description = "Turn the request into a refund proposal")
    public RefundProposal propose(ApprovalRequest request) {
        return new RefundProposal(request.orderNumber(), new java.math.BigDecimal(request.amount()), "Supervisor review");
    }

    @SecureAgentTool("hasRole('SUPERVISOR')")
    @AchievesGoal(description = "The refund has been approved by a supervisor")
    @Action(description = "Approve the refund", actionRetryPolicy = ActionRetryPolicy.FIRE_ONCE)
    public ApprovedRefund approve(RefundProposal proposal, OperationContext context) {
        String approver = context.user() == null ? "unknown" : context.user().getId();
        return new ApprovedRefund(proposal.orderNumber(), proposal.amount().toPlainString(), approver);
    }
}
