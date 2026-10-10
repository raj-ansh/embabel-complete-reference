package com.northwind.support.service;

import com.northwind.support.domain.SupportDomain.OrderSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderLookup {

    private final Map<String, OrderSummary> orders = Map.of(
            "NW-10442", new OrderSummary("NW-10442", "C-1001", "SHIPPED", LocalDate.of(2026, 10, 9), new BigDecimal("189.00")),
            "NW-10501", new OrderSummary("NW-10501", "C-1002", "PROCESSING", LocalDate.of(2026, 10, 14), new BigDecimal("149.00")));

    /**
     * Returns the order only if it belongs to the given customer. Someone else's order is
     * indistinguishable from a non-existent one: no information leaks either way.
     */
    public Optional<OrderSummary> findForCustomer(String orderNumber, String customerId) {
        return Optional.ofNullable(orders.get(orderNumber))
                .filter(order -> order.customerId().equals(customerId));
    }
}
