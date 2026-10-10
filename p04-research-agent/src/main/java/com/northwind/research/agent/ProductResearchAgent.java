package com.northwind.research.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.research.domain.Research.CompetitorPricing;
import com.northwind.research.domain.Research.ProductQuestion;
import com.northwind.research.domain.Research.ResearchBrief;
import com.northwind.research.domain.Research.ReviewSignal;
import com.northwind.research.domain.Research.SeasonalDemand;
import com.northwind.research.domain.Research.SupplierTerms;
import com.northwind.research.sources.ResearchSources;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * P4: a research-then-synthesise agent.
 * Four research actions gather optional evidence; one synthesis action writes the brief.
 * Which research actually happens depends on the planner (see ResearchRunner).
 */
@Agent(description = "Research whether NorthWind should stock a new product and recommend a decision")
public class ProductResearchAgent {

    private final ResearchSources sources;

    public ProductResearchAgent(ResearchSources sources) {
        this.sources = sources;
    }

    // --- research actions: high value, low cost, each runs at most once (canRerun defaults to false)

    @Action(description = "Get supplier cost, lead time and minimum order", value = 0.9, cost = 0.1)
    public SupplierTerms supplierTerms(ProductQuestion question) {
        return sources.supplierTerms(question.productName());
    }

    @Action(description = "Summarise customer review signals", value = 0.85, cost = 0.1)
    public ReviewSignal reviewSignal(ProductQuestion question) {
        return sources.reviews(question.productName());
    }

    @Action(description = "Collect competitor pricing", value = 0.8, cost = 0.1)
    public CompetitorPricing competitorPricing(ProductQuestion question) {
        return sources.competitorPricing(question.productName());
    }

    @Action(description = "Check seasonal demand for the category", value = 0.7, cost = 0.1)
    public SeasonalDemand seasonalDemand(ProductQuestion question) {
        return sources.seasonality(question.category());
    }

    // --- synthesis: lower value than research, so it runs once research is exhausted

    @AchievesGoal(description = "A stocking recommendation grounded in the available evidence")
    @Action(description = "Write the research brief", value = 0.5, cost = 0.1)
    public ResearchBrief synthesise(ProductQuestion question,
                                    @Nullable SupplierTerms supplier,
                                    @Nullable ReviewSignal reviews,
                                    @Nullable CompetitorPricing pricing,
                                    @Nullable SeasonalDemand demand,
                                    OperationContext context) {
        List<String> evidence = new ArrayList<>();
        if (supplier != null) evidence.add("Supplier: " + supplier);
        if (reviews != null) evidence.add("Reviews: " + reviews);
        if (pricing != null) evidence.add("Competitor pricing: " + pricing);
        if (demand != null) evidence.add("Seasonality: " + demand);

        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.2))
                .createObject("""
                        You are a merchandising analyst at NorthWind Outfitters.
                        Recommend whether to stock "%s" (%s). Use ONLY the evidence below.
                        If there is too little evidence to decide, recommend NEED_MORE_DATA.
                        List in evidenceUsed the evidence lines you relied on.

                        Evidence:
                        %s""".formatted(question.productName(), question.category(),
                        evidence.isEmpty() ? "(none)" : String.join("\n", evidence)), ResearchBrief.class);
    }
}
