package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.NativeStructuredOutputMode;
import com.northwind.llm.domain.Support.ProductSummary;
import org.springframework.stereotype.Service;

/** Chapter 76: typed output, with the native structured-output mode chosen explicitly. */
@Service
public class ProductSummaries {

    private final Ai ai;

    public ProductSummaries(Ai ai) {
        this.ai = ai;
    }

    public ProductSummary summarise(String productText, NativeStructuredOutputMode mode) {
        LlmOptions options = mode.applyTo(LlmOptions.withLlmForRole("balanced").withTemperature(0.0));
        return ai.withLlm(options).createObject("""
                Summarise the product described between the <product> tags for a product listing page.
                Use only facts from the description.

                <product>
                %s
                </product>""".formatted(productText), ProductSummary.class);
    }
}
