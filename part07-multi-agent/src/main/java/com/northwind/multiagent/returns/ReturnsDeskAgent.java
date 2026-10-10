package com.northwind.multiagent.returns;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
import com.northwind.multiagent.returns.Returns.Customer;
import com.northwind.multiagent.returns.Returns.EligibilityDecision;
import com.northwind.multiagent.returns.Returns.ReturnOutcome;
import com.northwind.multiagent.returns.Returns.ReturnRequest;

/**
 * Chapter 49: the returns desk delegates the policy decision to a subagent, then concludes.
 * The subagent is an ordinary Spring bean, injected like any other collaborator.
 */
@Agent(description = "Handle a customer's return request end to end")
public class ReturnsDeskAgent {

    private final ReturnEligibilityAgent eligibility;

    public ReturnsDeskAgent(ReturnEligibilityAgent eligibility) {
        this.eligibility = eligibility;
    }

    @Action(description = "Ask the eligibility specialist")
    public EligibilityDecision checkEligibility(ReturnRequest request, Customer customer) {
        // Chapter 51: Customer is unused here but DECLARED, because the subagent needs it. Without it,
        // this action is not even planned, rather than starting a subagent that would get stuck.
        // Runs ReturnEligibility as a nested process on a blackboard spawned from ours,
        // and returns the result of ITS goal as the result of this action.
        return RunSubagent.fromAnnotatedInstance(eligibility, EligibilityDecision.class);
    }

    @AchievesGoal(description = "The customer's return request has been handled")
    @Action(description = "Tell the customer the outcome")
    public ReturnOutcome conclude(ReturnRequest request, EligibilityDecision decision) {
        return decision.eligible()
                ? new ReturnOutcome(request.orderNumber(), "APPROVED",
                "Return approved (" + decision.rule() + "). Refund: $" + decision.refundAmount())
                : new ReturnOutcome(request.orderNumber(), "DECLINED", decision.rule());
    }
}
