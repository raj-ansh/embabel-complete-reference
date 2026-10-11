package com.northwind.llm.prompts;

import com.embabel.agent.api.annotation.LlmTool;

/** Chapter 74: the tool that the returns-policy reference tells the model to use. */
public class ReturnsPolicyTools {

    @LlmTool(description = "Check whether an item from an order can still be returned")
    public String checkEligibility(@LlmTool.Param(description = "Order number, e.g. NW-10442") String orderNumber,
                                   @LlmTool.Param(description = "Product SKU, e.g. TENT-2P") String sku) {
        return orderNumber.equals("NW-10442") ? "Eligible: delivered 12 days ago, within the 30-day window."
                : "Not found: check the order number.";
    }
}
