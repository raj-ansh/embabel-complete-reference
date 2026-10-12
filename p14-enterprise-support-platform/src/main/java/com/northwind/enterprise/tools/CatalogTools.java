package com.northwind.enterprise.tools;

import com.embabel.agent.api.annotation.LlmTool;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** P14: product data for the research agent. Read-only, fixed SQL, a bounded set of categories. */
@Component
public class CatalogTools {

    public record Product(String sku, String name, BigDecimal price, BigDecimal weightKg, BigDecimal rating) {
    }

    private static final Set<String> CATEGORIES = Set.of("tent", "stove", "bag");

    private final JdbcClient jdbc;

    public CatalogTools(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @LlmTool(description = "Products in a category (tent, stove or bag) with price, weight and rating")
    public List<Product> productsInCategory(@LlmTool.Param(description = "tent, stove or bag") String category) {
        if (!CATEGORIES.contains(category)) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT sku, name, price, weight_kg, rating FROM product
                        WHERE category = :category ORDER BY price""")
                .param("category", category)
                .query(Product.class)
                .list();
    }
}
