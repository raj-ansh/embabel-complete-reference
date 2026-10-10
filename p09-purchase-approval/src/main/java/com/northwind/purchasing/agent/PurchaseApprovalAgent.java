package com.northwind.purchasing.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.purchasing.domain.Purchasing.ApprovalDecision;
import com.northwind.purchasing.domain.Purchasing.PolicyCheck;
import com.northwind.purchasing.domain.Purchasing.PurchaseRequest;
import com.northwind.purchasing.domain.Purchasing.PurchaseResult;
import com.northwind.purchasing.domain.Purchasing.Requester;
import com.northwind.purchasing.domain.Purchasing.RiskNote;
import com.northwind.purchasing.domain.Purchasing.Route;
import com.northwind.purchasing.domain.PurchasingPolicy;

import java.util.UUID;

/**
 * P9: an AI purchase approval agent. Models read the request and brief the approver;
 * policy decides the route; a PERSON approves anything above the auto-approval limit.
 */
@Agent(description = "Turn an employee's purchase request into an approved purchase order or a rejection")
public class PurchaseApprovalAgent {

    static final String AUTO = "autoApprove";
    static final String HUMAN = "needsApproval";
    static final String REJECT = "rejectByPolicy";

    private final PurchasingPolicy policy;

    public PurchaseApprovalAgent(PurchasingPolicy policy) {
        this.policy = policy;
    }

    @Action(description = "Extract a structured purchase request")
    public PurchaseRequest extract(UserInput text, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("cheap").withTemperature(0.0))
                .createObject("""
                        Extract the purchase request: supplier, item, quantity, unit price (a number, no currency
                        symbol) and the business justification. If something is not stated, leave it empty.
                        <request>
                        %s
                        </request>""".formatted(text.getContent()), PurchaseRequest.class);
    }

    @Action(description = "Apply the purchasing policy", post = {AUTO, HUMAN, REJECT})
    public PolicyCheck checkPolicy(PurchaseRequest request) {
        var total = request.total();
        if (total.compareTo(policy.hardLimit()) > 0) {
            return new PolicyCheck(Route.REJECT, total, "Above the hard limit of " + policy.hardLimit() + "; raise a procurement project");
        }
        if (total.compareTo(policy.autoApproveLimit()) <= 0) {
            return new PolicyCheck(Route.AUTO_APPROVE, total, "Within the auto-approval limit");
        }
        return new PolicyCheck(Route.NEEDS_APPROVAL, total, "Above the auto-approval limit of " + policy.autoApproveLimit());
    }

    @Condition(name = AUTO)
    public boolean autoApprove(PolicyCheck check) {
        return check.route() == Route.AUTO_APPROVE;
    }

    @Condition(name = HUMAN)
    public boolean needsApproval(PolicyCheck check) {
        return check.route() == Route.NEEDS_APPROVAL;
    }

    @Condition(name = REJECT)
    public boolean rejectByPolicy(PolicyCheck check) {
        return check.route() == Route.REJECT;
    }

    @Action(pre = AUTO, description = "Approve by policy")
    public ApprovalDecision approveByPolicy(PolicyCheck check) {
        return new ApprovalDecision(true, "policy", check.reason());
    }

    @Action(pre = REJECT, description = "Reject by policy")
    public ApprovalDecision rejectByPolicy(PurchaseRequest request, PolicyCheck check) {
        return new ApprovalDecision(false, "policy", check.reason());
    }

    @Action(pre = HUMAN, description = "Brief the approver")
    public RiskNote assessRisk(PurchaseRequest request, Requester requester, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.2))
                .createObject("""
                        Brief a budget holder on this purchase request in two sentences, then list concerns
                        (unclear justification, unusual quantity, price that looks high). Do not recommend a decision.
                        <purchase department="%s">
                        %s x %d from %s at %s each. Justification: %s
                        </purchase>""".formatted(requester.department(), request.item(), request.quantity(),
                        request.supplier(), request.unitPrice(), request.justification()), RiskNote.class);
    }

    @Action(pre = HUMAN, description = "Ask a budget holder to decide")
    public ApprovalDecision requestApproval(PurchaseRequest request, PolicyCheck check, RiskNote risk) {
        // The process pauses here (status WAITING) until a person submits an ApprovalDecision.
        return WaitFor.formSubmission("""
                Approve %d x %s from %s, total %s?
                Policy: %s
                Briefing: %s
                Concerns: %s""".formatted(request.quantity(), request.item(), request.supplier(), check.total(),
                check.reason(), risk.summary(), String.join("; ", risk.concerns())), ApprovalDecision.class);
    }

    @AchievesGoal(description = "The purchase request has been decided")
    @Action(description = "Raise the purchase order or record the rejection")
    public PurchaseResult conclude(PurchaseRequest request, PolicyCheck check, ApprovalDecision decision) {
        return decision.approved()
                ? new PurchaseResult("ORDERED", "PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                check.total(), decision.decidedBy(), decision.comment())
                : new PurchaseResult("REJECTED", null, check.total(), decision.decidedBy(), decision.comment());
    }
}
