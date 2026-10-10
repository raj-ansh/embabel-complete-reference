package com.northwind.catalog.service;

import com.northwind.catalog.config.CatalogProperties;
import com.northwind.catalog.domain.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** An in-memory catalog. Later chapters replace it with Spring Data. */
@Service
public class ProductCatalog {

    private final List<Product> products = List.of(
            new Product("TENT-2P", "Ridgeline 2-Person Tent", "tents", new BigDecimal("189.00"), 14),
            new Product("TENT-4P", "Basecamp 4-Person Tent", "tents", new BigDecimal("329.00"), 0),
            new Product("BOOT-W9", "Trailblazer Waterproof Boot", "footwear", new BigDecimal("149.00"), 32),
            new Product("PACK-45", "Summit 45L Backpack", "packs", new BigDecimal("219.00"), 7));

    private final CatalogProperties properties;

    public ProductCatalog(CatalogProperties properties) {
        this.properties = properties;
    }

    public Optional<Product> findBySku(String sku) {
        return products.stream().filter(p -> p.sku().equalsIgnoreCase(sku)).findFirst();
    }

    public List<Product> search(String text, boolean inStockOnly) {
        String needle = text.toLowerCase(Locale.ROOT);
        return products.stream()
                .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(needle)
                        || p.category().equalsIgnoreCase(needle))
                .filter(p -> !inStockOnly || p.inStock())
                .limit(properties.maxResults())
                .toList();
    }

    public int size() {
        return products.size();
    }
}
