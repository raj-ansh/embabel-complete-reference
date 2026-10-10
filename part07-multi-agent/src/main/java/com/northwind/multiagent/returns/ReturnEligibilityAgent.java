package com.northwind.multiagent.returns;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.northwind.multiagent.returns.Returns.Customer;
import com.northwind.multiagent.returns.Returns.EligibilityDecision;
import com.northwind.multiagent.returns.Returns.OrderFacts;
import com.northwind.multiagent.returns.Returns.ReturnRequest;
import com.northwind.multiagent.returns.Returns.Tier;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Chapter 49: a specialist agent. It knows one thing, NorthWind's return policy, and has its own
 * two-step plan. It can run on its own or as a subagent of the returns desk.
 * Deterministic on purpose: a policy decision is not a job for a model.
 */
@Agent(name = "ReturnEligibility", description = "Decide whether a return is eligible under NorthWind's return policy")
public class ReturnEligibilityAgent {

    private static final Map<String, OrderFacts> ORDERS = Map.of(
            "NW-10442", new OrderFacts("NW-10442", "BOOT-W9", false, new BigDecimal("149.00"), true),
            "NW-10518", new OrderFacts("NW-10518", "CLR-JKT", true, new BigDecimal("89.00"), true));

    @Action(description = "Look up the order being returned")
    public OrderFacts lookupOrder(ReturnRequest request) {
        // Chapter 51: an expected failure is a VALUE the plan can reason about, not an exception that ends it
        return ORDERS.getOrDefault(request.orderNumber(), OrderFacts.notFound(request.orderNumber()));
    }

    @AchievesGoal(description = "The return's eligibility has been decided")
    @Action(description = "Apply the return policy")
    public EligibilityDecision decide(ReturnRequest request, OrderFacts order, Customer customer) {
        int window = customer.tier() == Tier.GOLD ? 60 : 30;     // Customer comes from the PARENT's blackboard
        if (!order.found()) {
            return new EligibilityDecision(false, "We could not find order " + order.orderNumber(), BigDecimal.ZERO);
        }
        if (order.finalSale()) {
            return new EligibilityDecision(false, "Final-sale items cannot be returned", BigDecimal.ZERO);
        }
        if ("damaged".equalsIgnoreCase(request.reason()) && request.daysSinceDelivery() <= 90) {
            return new EligibilityDecision(true, "Damaged items: full refund within 90 days", order.paid());
        }
        if (request.daysSinceDelivery() > window) {
            return new EligibilityDecision(false, "Outside the " + window + "-day return window", BigDecimal.ZERO);
        }
        return new EligibilityDecision(true, "Within the " + window + "-day return window", order.paid());
    }
}
