package com.northwind.springbasics.service;

import com.northwind.springbasics.domain.Order;

import java.math.BigDecimal;

/** A strategy for discounting an order. Callers depend on this interface, not on an implementation. */
public interface DiscountPolicy {

    BigDecimal discountFor(Order order);
}
