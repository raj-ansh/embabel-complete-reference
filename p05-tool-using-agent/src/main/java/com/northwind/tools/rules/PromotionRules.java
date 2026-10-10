package com.northwind.tools.rules;

import com.embabel.agent.api.annotation.LlmTool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool 10: a small business-rule engine. Rules are evaluated in code, in order, and the result
 * explains which rules applied, so the model can explain the outcome without inventing it.
 */
public class PromotionRules {

    public record Cart(String customerTier, BigDecimal subtotal, int items, boolean firstOrder) {
    }

    public record PromotionResult(BigDecimal discountPercent, boolean freeShipping, List<String> rulesApplied) {
    }

    @LlmTool(description = "Evaluate NorthWind's promotion rules for a cart and explain which rules applied")
    public PromotionResult evaluatePromotions(
            @LlmTool.Param(description = "Customer tier: STANDARD, SILVER or GOLD") String customerTier,
            @LlmTool.Param(description = "Cart subtotal in USD") BigDecimal subtotal,
            @LlmTool.Param(description = "Number of items in the cart") int items,
            @LlmTool.Param(description = "Whether this is the customer's first order") boolean firstOrder) {
        Cart cart = new Cart(customerTier, subtotal, items, firstOrder);
        BigDecimal discount = BigDecimal.ZERO;
        boolean freeShipping = false;
        List<String> applied = new ArrayList<>();

        if ("GOLD".equalsIgnoreCase(cart.customerTier())) {
            discount = discount.add(BigDecimal.TEN);
            applied.add("GOLD members get 10% off");
        }
        if (cart.firstOrder()) {
            discount = discount.add(BigDecimal.valueOf(5));
            applied.add("First order: extra 5% off");
        }
        if (cart.subtotal().compareTo(BigDecimal.valueOf(150)) >= 0) {
            freeShipping = true;
            applied.add("Orders of $150 or more ship free");
        }
        if (discount.compareTo(BigDecimal.valueOf(15)) > 0) {
            discount = BigDecimal.valueOf(15);
            applied.add("Discounts are capped at 15%");
        }
        return new PromotionResult(discount, freeShipping, applied);
    }
}
