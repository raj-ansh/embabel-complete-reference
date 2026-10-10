package com.northwind.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Methods a model may ask the application to call. The application, not the model, executes them. */
@Component
public class OrderStatusTools {

    private final Map<String, String> statuses = Map.of(
            "NW-10442", "SHIPPED on 2026-10-01, carrier ParcelFast, tracking PF-883120",
            "NW-10501", "PROCESSING, expected to ship within 2 business days");

    @Tool(description = "Look up the current shipping status of a NorthWind order by its order number")
    public String orderStatus(@ToolParam(description = "Order number, for example NW-10442") String orderNumber) {
        return statuses.getOrDefault(orderNumber, "UNKNOWN order " + orderNumber);
    }
}
