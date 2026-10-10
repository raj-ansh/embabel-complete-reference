package com.northwind.core.blackboard;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RequireNameMatch;
import com.embabel.agent.api.common.OperationContext;

import java.math.BigDecimal;

/**
 * Chapter 28: the blackboard and binding.
 * Two objects of the SAME type are told apart by name, and the context can bind extra objects.
 */
@Agent(description = "Compare NorthWind's price for a product with a competitor's")
public class PriceComparisonAgent {

    public record ProductCode(String sku) {
    }

    public record Price(String source, BigDecimal amount) {
    }

    public record PriceGap(String sku, BigDecimal difference, boolean weAreCheaper) {
    }

    public record AuditNote(String text) {
    }

    @Action(outputBinding = "ourPrice")
    public Price ourPrice(ProductCode product) {
        return new Price("northwind", new BigDecimal("189.00"));
    }

    @Action(outputBinding = "theirPrice")
    public Price theirPrice(ProductCode product, OperationContext context) {
        context.bind("audit", new AuditNote("Competitor price looked up for " + product.sku()));
        return new Price("competitor", new BigDecimal("199.00"));
    }

    @AchievesGoal(description = "Price gap calculated")
    @Action
    public PriceGap compare(ProductCode product,
                            @RequireNameMatch Price ourPrice,
                            @RequireNameMatch Price theirPrice) {
        BigDecimal diff = theirPrice.amount().subtract(ourPrice.amount());
        return new PriceGap(product.sku(), diff, diff.signum() > 0);
    }
}
