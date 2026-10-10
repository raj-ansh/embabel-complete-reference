package com.northwind.support.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** A stand-in for the order system, shared by the billing and order specialists. */
@Component
public class OrderBook {

    public record Order(String orderNumber, String customerId, String item, BigDecimal paid, String status) {
    }

    private static final Map<String, Order> ORDERS = Map.of(
            "NW-10442", new Order("NW-10442", "C-1001", "Trailblazer Waterproof Boot", new BigDecimal("149.00"), "SHIPPED"),
            "NW-10601", new Order("NW-10601", "C-1001", "Basecamp 4-Person Tent", new BigDecimal("329.00"), "DELIVERED"),
            "NW-10518", new Order("NW-10518", "C-1002", "Glacier Insulated Jacket", new BigDecimal("229.00"), "DELIVERED"));

    /** Ownership is part of the lookup: another customer's order is simply not found. */
    public Optional<Order> find(String orderNumber, String customerId) {
        return Optional.ofNullable(ORDERS.get(orderNumber)).filter(o -> o.customerId().equals(customerId));
    }
}
