package com.northwind.llm.tools;

import com.embabel.agent.api.annotation.LlmTool;

/** Chapter 79: an order lookup whose raw result contains personal data the model does not need. */
public class OrderTools {

    @LlmTool(description = "Get the delivery status of an order")
    public String orderStatus(@LlmTool.Param(description = "Order number in the form NW-12345") String orderNumber) {
        if (!orderNumber.equals("NW-10442")) {
            return "No order " + orderNumber + " was found.";
        }
        return "NW-10442 SHIPPED via Parcelforce, tracking PF123456789GB, expected 14 October. "
                + "Customer contact: asha.patel@example.com";
    }
}
