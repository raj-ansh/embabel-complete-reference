package com.northwind.catalog;

import com.northwind.catalog.config.CatalogProperties;
import com.northwind.catalog.service.ProductCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts the full application with the "local" profile and checks configuration binding. */
@SpringBootTest
@ActiveProfiles("local")
class CatalogApplicationTest {

    @Autowired
    CatalogProperties properties;

    @Autowired
    ProductCatalog catalog;

    @Test
    void localProfileOverridesMaxResults() {
        assertThat(properties.storeName()).isEqualTo("NorthWind Outfitters");
        assertThat(properties.maxResults()).isEqualTo(5);           // from application-local.yml
        assertThat(catalog.search("tents", false)).hasSize(2);
    }
}
