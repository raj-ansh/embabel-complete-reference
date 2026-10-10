package com.northwind.research.sources;

import com.northwind.research.domain.Research.CompetitorPricing;
import com.northwind.research.domain.Research.ReviewSignal;
import com.northwind.research.domain.Research.SeasonalDemand;
import com.northwind.research.domain.Research.SupplierTerms;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-ins for the external systems a merchandising researcher would consult.
 * In production each would be an HTTP client or database query, possibly behind a tool.
 */
@Service
public class ResearchSources {

    private final AtomicInteger lookups = new AtomicInteger();

    public SupplierTerms supplierTerms(String product) {
        lookups.incrementAndGet();
        return new SupplierTerms("Alpine Supply Co.", new BigDecimal("41.50"), 21, 200);
    }

    public ReviewSignal reviews(String product) {
        lookups.incrementAndGet();
        return new ReviewSignal(4.4, 1280, List.of("zip snags in cold weather"));
    }

    public CompetitorPricing competitorPricing(String product) {
        lookups.incrementAndGet();
        return new CompetitorPricing(new BigDecimal("89.00"), new BigDecimal("119.00"), 6);
    }

    public SeasonalDemand seasonality(String category) {
        lookups.incrementAndGet();
        return new SeasonalDemand("October to February", 140);
    }

    public int lookups() {
        return lookups.get();
    }

    public void reset() {
        lookups.set(0);
    }
}
