package com.northwind.planning.goap;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;

import java.math.BigDecimal;

/**
 * Chapter 32: GOAP chooses the cheapest plan. Two actions can produce a CarrierRate;
 * the cached rate table is cheaper, but only applies to standard parcels.
 */
@Agent(description = "Quote shipping for a NorthWind parcel")
public class ShippingQuoteAgent {

    public record Parcel(String destinationZone, double weightKg, boolean oversized) {
    }

    public record CarrierRate(String source, BigDecimal amount) {
    }

    public record ShippingQuote(BigDecimal price, String rateSource) {
    }

    @Action(description = "Look up the cached rate table", cost = 0.1,
            pre = {"spel:!parcel.oversized"})
    public CarrierRate fromRateTable(Parcel parcel) {
        return new CarrierRate("rate-table", BigDecimal.valueOf(6.5 + parcel.weightKg()));
    }

    @Action(description = "Call the carrier's live quoting API (slow, metered)", cost = 0.7)
    public CarrierRate fromCarrierApi(Parcel parcel) {
        return new CarrierRate("carrier-api", BigDecimal.valueOf(9.0 + 1.5 * parcel.weightKg()));
    }

    @AchievesGoal(description = "The customer has a shipping quote")
    @Action
    public ShippingQuote quote(CarrierRate rate) {
        return new ShippingQuote(rate.amount(), rate.source());
    }
}
