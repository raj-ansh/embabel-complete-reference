package com.northwind.tools.customer;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;

import java.util.Map;

/**
 * Tool 3: customer lookup. The model never chooses WHICH customer: the customer id comes from
 * ToolCallContext, set by the application from the authenticated session. The model sees no parameters.
 */
public class CustomerTools {

    public record CustomerSummary(String firstName, String tier, int loyaltyPoints) {
    }

    private final Map<String, CustomerSummary> customers = Map.of(
            "C-1001", new CustomerSummary("Asha", "GOLD", 4200),
            "C-1002", new CustomerSummary("Mateo", "STANDARD", 350));

    @LlmTool(description = "Get the signed-in customer's name, loyalty tier and points")
    public CustomerSummary currentCustomer(ToolCallContext context) {
        String customerId = context.get("customerId");
        if (customerId == null) {
            throw new IllegalStateException("No customerId in tool call context");
        }
        CustomerSummary summary = customers.get(customerId);
        if (summary == null) {
            throw new IllegalStateException("Unknown customer");
        }
        return summary;
    }
}
