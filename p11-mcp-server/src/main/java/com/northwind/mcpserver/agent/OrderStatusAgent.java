package com.northwind.mcpserver.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.northwind.mcpserver.domain.Catalog.OrderQuery;
import com.northwind.mcpserver.domain.Catalog.OrderStatus;

import java.util.Map;

/**
 * P11: a goal published as an MCP tool. No model is involved: an MCP client gets a deterministic,
 * typed answer from an agent whose plan happens to be one step.
 */
@Agent(description = "Look up the delivery status of a NorthWind order")
public class OrderStatusAgent {

    private final Map<String, OrderStatus> orders = Map.of(
            "NW-10442", new OrderStatus("NW-10442", "SHIPPED", "14 October"),
            "NW-10501", new OrderStatus("NW-10501", "PROCESSING", "17 October"));

    @AchievesGoal(description = "Get the delivery status of a NorthWind order from its order number (NW-12345)",
            export = @Export(remote = true, name = "orderStatus", startingInputTypes = {OrderQuery.class}))
    @Action(description = "Look up the order")
    public OrderStatus lookUp(OrderQuery query) {
        return orders.getOrDefault(query.orderNumber(), new OrderStatus(query.orderNumber(), "NOT_FOUND", "-"));
    }
}
