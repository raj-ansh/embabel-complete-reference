package com.northwind.catalog.domain;

import java.math.BigDecimal;

public record Product(String sku, String name, String category, BigDecimal price, int stock) {

    public boolean inStock() {
        return stock > 0;
    }
}
