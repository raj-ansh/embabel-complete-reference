package com.northwind.returns.service;

import com.northwind.returns.domain.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stand-in for NorthWind's order system. */
@Service
public class OrderService {

    private final Map<String, Order> orders = Map.of(
            "NW-10442", new Order("NW-10442", "Asha", LocalDate.now().minusDays(6), List.of(
                    new Order.Line("TENT-2P", "Ridgeline 2-Person Tent", new BigDecimal("189.00"), false))),
            "NW-09001", new Order("NW-09001", "Mateo", LocalDate.now().minusDays(75), List.of(
                    new Order.Line("BOOT-W9", "Trailblazer Waterproof Boot", new BigDecimal("149.00"), false))),
            "NW-10777", new Order("NW-10777", "Lin", LocalDate.now().minusDays(3), List.of(
                    new Order.Line("SALE-JKT", "Clearance Shell Jacket", new BigDecimal("59.00"), true))));

    public Optional<Order> findByNumber(String number) {
        return Optional.ofNullable(orders.get(number));
    }
}
