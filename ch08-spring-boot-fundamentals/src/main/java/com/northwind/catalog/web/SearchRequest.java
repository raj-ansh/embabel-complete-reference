package com.northwind.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/products/search. Validated before the controller method runs. */
public record SearchRequest(
        @NotBlank @Size(max = 100) String text,
        boolean inStockOnly) {
}
