package com.northwind.advanced.troubleshooting;

import com.embabel.agent.api.annotation.LlmTool;
import org.springframework.stereotype.Component;

/** Chapter 118: a tool whose backing service is down. What does the model see? */
@Component
public class OrderLookupTools {

    @LlmTool(description = "Status of an order")
    public String orderStatus(@LlmTool.Param(description = "Order number, e.g. NW-40001") String orderNumber) {
        throw new IllegalStateException("Order service unavailable (connection refused to orders:8080)");
    }
}
