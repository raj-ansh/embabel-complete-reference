package com.northwind.llm.tools;

import com.embabel.agent.api.annotation.LlmTool;

import java.util.Map;

/** Chapter 70: one simple tool, enough to show how a small model can stall after a tool call. */
public class StockTools {

    private final Map<String, String> stock = Map.of(
            "TENT-2P", "TENT-2P: 14 in stock at the Leeds warehouse",
            "STOVE-1", "STOVE-1: out of stock, next delivery 20 October");

    @LlmTool(description = "Current stock level for a product SKU")
    public String stockLevel(@LlmTool.Param(description = "Product SKU, for example TENT-2P") String sku) {
        return stock.getOrDefault(sku, sku + ": unknown SKU");
    }
}
