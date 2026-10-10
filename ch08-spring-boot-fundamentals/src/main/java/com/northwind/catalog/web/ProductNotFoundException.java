package com.northwind.catalog.web;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("No product with SKU " + sku);
    }
}
