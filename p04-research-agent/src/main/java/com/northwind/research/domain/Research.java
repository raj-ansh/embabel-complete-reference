package com.northwind.research.domain;

import java.math.BigDecimal;
import java.util.List;

/** The research agent's vocabulary. */
public final class Research {

    private Research() {
    }

    /** The merchandising team's question: should we stock this product? */
    public record ProductQuestion(String productName, String category) {
    }

    /** Evidence gathered by research actions. Each is optional for the final brief. */
    public record SupplierTerms(String supplier, BigDecimal unitCost, int leadTimeDays, int minimumOrder) {
    }

    public record ReviewSignal(double averageRating, int reviewCount, List<String> commonComplaints) {
    }

    public record CompetitorPricing(BigDecimal lowestPrice, BigDecimal highestPrice, int competitorsStocking) {
    }

    public record SeasonalDemand(String peakMonths, int relativeDemandIndex) {
    }

    /** The goal: a recommendation grounded in whatever evidence was gathered. */
    public record ResearchBrief(Recommendation recommendation, String rationale, List<String> evidenceUsed) {

        public enum Recommendation { STOCK, TRIAL_ORDER, DO_NOT_STOCK, NEED_MORE_DATA }
    }
}
