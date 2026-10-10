package com.northwind.core.domain;

import org.springframework.ai.tool.annotation.Tool;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Chapter 26: a domain object with behaviour. Selected methods are exposed to the model as tools;
 * the rest stay internal. Calculations the model might get wrong are done here, in code.
 */
public record LoyaltyMember(String memberId, String firstName, int points, int yearsAsMember) {

    @Tool(description = "The member's loyalty discount as a percentage, based on points and tenure")
    public BigDecimal loyaltyDiscountPercent() {
        BigDecimal base = BigDecimal.valueOf(Math.min(points / 1000, 10));       // 1% per 1000 points, max 10%
        BigDecimal tenure = yearsAsMember >= 5 ? BigDecimal.valueOf(2) : BigDecimal.ZERO;
        return base.add(tenure).setScale(1, RoundingMode.HALF_UP);
    }

    @Tool(description = "Whether the member qualifies for the premium events programme")
    public boolean premiumEventsEligible() {
        return points >= 5000 || yearsAsMember >= 3;
    }

    /** Not annotated: never offered to the model. */
    public LoyaltyMember withPointsAdded(int extra) {
        return new LoyaltyMember(memberId, firstName, points + extra, yearsAsMember);
    }
}
