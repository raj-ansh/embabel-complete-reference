package com.northwind.springbasics.domain;

import java.math.BigDecimal;

/** An order line as the pricing service sees it. Immutable. */
public record Order(String number, String sku, int quantity, BigDecimal unitPrice) {

    public Order {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
