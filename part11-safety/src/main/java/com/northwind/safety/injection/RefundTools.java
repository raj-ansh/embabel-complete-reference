package com.northwind.safety.injection;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Chapter 82: the model may ask for any refund it likes; the tool decides. Ownership and limits are
 * checked in code, against the caller's identity from ToolCallContext, never against anything the
 * model says.
 */
public class RefundTools {

    public static final BigDecimal ASSISTANT_LIMIT = new BigDecimal("50");

    private record Order(String customerId, BigDecimal total) {
    }

    private final Map<String, Order> orders = Map.of(
            "NW-10442", new Order("C-1001", new BigDecimal("189.00")),
            "NW-10501", new Order("C-1002", new BigDecimal("64.50")));

    @LlmTool(description = "Refund part or all of a damaged order for the signed-in customer")
    public String issueRefund(@LlmTool.Param(description = "Order number, e.g. NW-10442") String orderNumber,
                              @LlmTool.Param(description = "Refund amount in pounds") BigDecimal amount,
                              ToolCallContext context) {
        Order order = orders.get(orderNumber);
        if (order == null || !order.customerId().equals(context.get("customerId"))) {
            return "REFUSED: no such order on this customer's account";
        }
        if (amount.compareTo(ASSISTANT_LIMIT) > 0 || amount.compareTo(order.total()) > 0) {
            return "REFUSED: the assistant may refund at most £" + ASSISTANT_LIMIT + "; a person will review this request";
        }
        return "REFUNDED £" + amount + " on " + orderNumber;
    }
}
