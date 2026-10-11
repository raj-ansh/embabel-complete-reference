package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.northwind.llm.tools.StockTools;
import org.springframework.stereotype.Service;

/** Chapter 70: a stock question answered by the cheap model with one tool. */
@Service
public class StockQuestions {

    private final Ai ai;

    public StockQuestions(Ai ai) {
        this.ai = ai;
    }

    public String ask(String productName, String sku) {
        return ai.withLlmByRole("cheap")
                .withToolObject(new StockTools())
                .generateText("Is the %s (%s) in stock? Use the stockLevel tool.".formatted(productName, sku));
    }
}
