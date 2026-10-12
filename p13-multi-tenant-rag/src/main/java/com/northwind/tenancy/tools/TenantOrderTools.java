package com.northwind.tenancy.tools;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chapter 104: one tool serving every tenant. The model supplies only the order number; the tenant and the
 * user come from tool call context, set by the application from the verified token. Every lookup is scoped
 * by both, so no prompt can reach another tenant's or another customer's orders.
 */
@Component
public class TenantOrderTools {

    private record Order(String tenant, String customer, String status) {
    }

    private final Map<String, Order> orders = Map.of(
            "NW-20001", new Order("acme-outdoors", "asha", "SHIPPED, arriving Thursday"),
            "NW-20002", new Order("acme-outdoors", "ben", "PROCESSING"),
            "NW-30001", new Order("peak-gear", "chloe", "DELIVERED on Monday"));

    @LlmTool(description = "Status of one of the signed-in customer's orders")
    public String orderStatus(@LlmTool.Param(description = "Order number, e.g. NW-20001") String orderNumber,
                              ToolCallContext context) {
        Order order = orders.get(orderNumber);
        if (order == null || !order.tenant().equals(context.get("tenant"))
                || !order.customer().equals(context.get("user"))) {
            return "No order " + orderNumber + " was found on your account.";      // same answer for every miss
        }
        return orderNumber + ": " + order.status();
    }
}
