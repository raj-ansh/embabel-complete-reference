package com.northwind.catalog.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration bound from the northwind.catalog.* properties.
 * Invalid values stop the application at startup instead of failing later.
 */
@Validated
@ConfigurationProperties(prefix = "northwind.catalog")
public record CatalogProperties(
        @NotBlank String storeName,
        @Min(1) @Max(100) int maxResults,
        @NotBlank String currency) {
}
