package com.northwind.production.service;

import com.northwind.production.domain.Issues.OrderFacts;

import java.util.Optional;

/** Part XIII: NorthWind's order system, as the agent sees it. */
public interface OrderService {

    Optional<OrderFacts> find(String orderNumber);
}
