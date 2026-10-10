package com.northwind.catalog.health;

import com.northwind.catalog.service.ProductCatalog;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Contributes a "catalog" entry to /actuator/health. */
@Component
public class CatalogHealthIndicator implements HealthIndicator {

    private final ProductCatalog catalog;

    public CatalogHealthIndicator(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Health health() {
        int size = catalog.size();
        return size > 0
                ? Health.up().withDetail("products", size).build()
                : Health.down().withDetail("reason", "catalog is empty").build();
    }
}
