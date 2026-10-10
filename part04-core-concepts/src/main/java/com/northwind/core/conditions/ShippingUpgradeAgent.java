package com.northwind.core.conditions;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;

import java.math.BigDecimal;

/**
 * Chapter 25: conditions that types alone cannot express.
 * Both actions accept a CartTotal; conditions decide which one may run.
 */
@Agent(description = "Decide whether to offer a NorthWind customer a free shipping upgrade")
public class ShippingUpgradeAgent {

    public static final String HIGH_VALUE = "highValueCart";

    public record CartTotal(BigDecimal amount, boolean expressAvailable) {
    }

    public record ShippingDecision(String offer, String reason) {
    }

    /** A named condition implemented in Java. No side effects: it may be evaluated many times. */
    @Condition(name = HIGH_VALUE)
    public boolean highValueCart(CartTotal cart) {
        return cart.amount().compareTo(new BigDecimal("250.00")) >= 0;
    }

    @AchievesGoal(description = "Customer offered free express shipping")
    @Action(pre = {HIGH_VALUE, "spel:cartTotal.expressAvailable"})
    public ShippingDecision offerFreeExpress(CartTotal cart) {
        return new ShippingDecision("FREE_EXPRESS", "Cart total " + cart.amount() + " qualifies");
    }

    @AchievesGoal(description = "Customer keeps standard shipping")
    @Action(pre = {"spel:cartTotal.amount < 250 || !cartTotal.expressAvailable"})
    public ShippingDecision keepStandard(CartTotal cart) {
        return new ShippingDecision("STANDARD", "No upgrade for this cart");
    }
}
