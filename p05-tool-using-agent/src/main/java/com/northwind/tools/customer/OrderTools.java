package com.northwind.tools.customer;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Tool 4: order lookup with an ownership check. The model supplies the order number (from the
 * conversation); the application supplies the customer (from ToolCallContext).
 */
public class OrderTools {

    public record OrderStatus(String orderNumber, String status, LocalDate expectedDelivery) {
    }

    private record StoredOrder(String customerId, OrderStatus status) {
    }

    private final Map<String, StoredOrder> orders = Map.of(
            "NW-10442", new StoredOrder("C-1001", new OrderStatus("NW-10442", "SHIPPED", LocalDate.of(2026, 10, 9))),
            "NW-10501", new StoredOrder("C-1002", new OrderStatus("NW-10501", "PROCESSING", LocalDate.of(2026, 10, 14))));

    @LlmTool(description = "Get the status of one of the signed-in customer's orders")
    public String orderStatus(@LlmTool.Param(description = "Order number in the form NW-12345") String orderNumber,
                              ToolCallContext context) {
        if (!orderNumber.matches("NW-\\d{4,6}")) {
            return "That is not a valid NorthWind order number.";
        }
        StoredOrder order = orders.get(orderNumber);
        if (order == null || !order.customerId().equals(context.get("customerId"))) {
            return "No order " + orderNumber + " was found on this customer's account.";   // same answer either way
        }
        return order.status().toString();
    }

    @LlmTool(description = "List the order numbers on the signed-in customer's account")
    public List<String> myOrders(ToolCallContext context) {
        Object customerId = context.get("customerId");
        return orders.entrySet().stream()
                .filter(e -> e.getValue().customerId().equals(customerId))
                .map(Map.Entry::getKey).sorted().toList();
    }
}
