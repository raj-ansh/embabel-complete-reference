package com.northwind.llm.cost;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** Chapter 80: each tenant's spending limit in US dollars, from configuration. */
@ConfigurationProperties("northwind.budgets")
public record TenantBudgets(Map<String, Double> monthlyUsd, double defaultMonthlyUsd) {

    public double limitFor(String tenant) {
        return monthlyUsd == null ? defaultMonthlyUsd : monthlyUsd.getOrDefault(tenant, defaultMonthlyUsd);
    }
}
