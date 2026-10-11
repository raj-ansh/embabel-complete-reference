package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.streaming.StreamingPromptRunnerBuilder;
import com.northwind.llm.domain.Support.ProductSummary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Chapter 77: typed objects, each delivered as soon as the model has finished writing it. */
@Service
public class HighlightStream {

    private final Ai ai;

    public HighlightStream(Ai ai) {
        this.ai = ai;
    }

    public Flux<ProductSummary> bestSellers(int count) {
        return new StreamingPromptRunnerBuilder(ai.withLlmByRole("balanced"))
                .streaming()
                .withPrompt("List the product highlights for NorthWind's %d best-selling products.".formatted(count))
                .createObjectStream(ProductSummary.class);
    }
}
