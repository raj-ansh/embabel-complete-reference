package com.northwind.mcpserver.tools;

import com.embabel.agent.api.annotation.LlmTool;

import java.util.Map;

/** P11: ordinary Embabel tools, published over MCP alongside the agents. Read-only by design. */
public class CatalogTools {

    private final Map<String, String> products = Map.of(
            "TENT-2P", "Ridgeline 2-Person Tent: 2.1 kg, 3,000 mm flysheet, two doors, £199",
            "STOVE-1", "Trailhead Stove: 220 g, boils a litre in four minutes, £49",
            "BAG-15", "Summit 15 Sleeping Bag: comfort rated to 15 °C, 840 g, £129");

    private final Map<String, Integer> stock = Map.of("TENT-2P", 14, "STOVE-1", 0, "BAG-15", 31);

    @LlmTool(description = "Product details for a NorthWind SKU")
    public String productDetails(@LlmTool.Param(description = "SKU, e.g. TENT-2P") String sku) {
        return products.getOrDefault(sku, "Unknown SKU " + sku);
    }

    @LlmTool(description = "Units in stock for a NorthWind SKU")
    public String stockLevel(@LlmTool.Param(description = "SKU, e.g. TENT-2P") String sku) {
        Integer units = stock.get(sku);
        return units == null ? "Unknown SKU " + sku : sku + ": " + units + " in stock";
    }
}
