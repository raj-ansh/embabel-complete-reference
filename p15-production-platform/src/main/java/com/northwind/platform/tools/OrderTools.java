package com.northwind.platform.tools;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** P15: order status, scoped by tenant and customer from tool call context (Chapters 46, 47 and 104). */
@Component
public class OrderTools {

    private final JdbcClient jdbc;

    public OrderTools(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @LlmTool(description = "Status of one of the signed-in customer's orders")
    public String orderStatus(@LlmTool.Param(description = "Order number, e.g. NW-40001") String orderNumber,
                              ToolCallContext context) {
        return jdbc.sql("""
                        SELECT status FROM orders
                        WHERE tenant = :tenant AND order_number = :number AND customer_id = :customer""")
                .param("tenant", context.get("tenant"))
                .param("number", orderNumber)
                .param("customer", context.get("customerId"))
                .query(String.class)
                .optional()
                .map(status -> orderNumber + ": " + status)
                .orElse("No order " + orderNumber + " was found on your account.");
    }
}
