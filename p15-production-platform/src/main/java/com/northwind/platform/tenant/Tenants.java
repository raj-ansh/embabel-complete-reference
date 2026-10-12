package com.northwind.platform.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Chapter 103: the platform's tenants, from configuration. In production this is a table managed by
 * NorthWind's onboarding service; the shape is what matters.
 */
@ConfigurationProperties("northwind")
public record Tenants(Map<String, Settings> tenants) {

    public enum Plan { BASIC, PREMIUM }

    /** Everything that may differ between tenants (Chapter 106). */
    public record Settings(String name, Plan plan, BigDecimal dailyBudgetUsd, String voice, boolean active) {
    }

    public Settings get(String tenantId) {
        return tenants.get(tenantId);
    }
}
