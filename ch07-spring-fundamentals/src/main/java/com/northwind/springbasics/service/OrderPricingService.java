package com.northwind.springbasics.service;

import com.northwind.springbasics.domain.Order;
import com.northwind.springbasics.event.OrderPricedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Prices orders. Knows nothing about how its collaborators are created. */
@Service
public class OrderPricingService {

    private final DiscountPolicy discountPolicy;
    private final ApplicationEventPublisher events;

    public OrderPricingService(DiscountPolicy discountPolicy, ApplicationEventPublisher events) {
        this.discountPolicy = discountPolicy;
        this.events = events;
    }

    public BigDecimal price(Order order) {
        BigDecimal total = order.subtotal().subtract(discountPolicy.discountFor(order));
        events.publishEvent(new OrderPricedEvent(order.number(), total));
        return total;
    }
}
