package com.northwind.production.service;

import com.embabel.agent.observability.annotation.TrackType;
import com.embabel.agent.observability.annotation.Tracked;
import com.northwind.production.domain.Issues.OrderFacts;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Part XIII: a small in-memory order system. In production this is a call to another service. */
@Service
public class InMemoryOrderService implements OrderService {

    private final Map<String, OrderFacts> orders = Map.of(
            "NW-10442", new OrderFacts("NW-10442", "C-1001", "DELIVERED", new BigDecimal("129.00")),
            "NW-10501", new OrderFacts("NW-10501", "C-1001", "IN_TRANSIT", new BigDecimal("349.00")),
            "NW-10577", new OrderFacts("NW-10577", "C-2002", "DELIVERED", new BigDecimal("59.00")));

    @Override
    @Tracked(value = "findOrder", type = TrackType.EXTERNAL_CALL, description = "Order system lookup")  // Chapter 92
    public Optional<OrderFacts> find(String orderNumber) {
        return Optional.ofNullable(orders.get(orderNumber));
    }
}
