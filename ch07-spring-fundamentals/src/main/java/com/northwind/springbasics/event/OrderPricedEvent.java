package com.northwind.springbasics.event;

import java.math.BigDecimal;

/** Published after an order has been priced. Any bean may listen for it. */
public record OrderPricedEvent(String orderNumber, BigDecimal total) {
}
