package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.Thinking;
import com.embabel.common.core.thinking.ThinkingResponse;
import com.northwind.llm.domain.Support.ProductSummary;
import org.springframework.stereotype.Service;

/** Chapter 78: a recommendation whose reasoning is kept for review, and may legitimately fail. */
@Service
public class TentAdvisor {

    private final Ai ai;

    public TentAdvisor(Ai ai) {
        this.ai = ai;
    }

    public ThinkingResponse<ProductSummary> recommend(String requirement) {
        return ai.withLlmByRole("best")
                .thinking()
                .createObjectIfPossible(prompt(requirement), ProductSummary.class);
    }

    /** Keep only the reasoning written in analysis tags; drop the model's scratch notes. */
    public ThinkingResponse<ProductSummary> recommendWithAnalysisOnly(String requirement) {
        return ai.withLlm(LlmOptions.withLlmForRole("best").withThinking(Thinking.withIncludedTags("analysis")))
                .thinking()
                .createObjectIfPossible(prompt(requirement), ProductSummary.class);
    }

    private static String prompt(String requirement) {
        return """
                Recommend one tent from the NorthWind catalogue for the requirement between the <requirement> tags.
                If no tent meets it, say so rather than inventing a product.

                <requirement>
                %s
                </requirement>""".formatted(requirement);
    }
}
