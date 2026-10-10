package com.northwind.purchasing.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Approval limits live in configuration, not in prompts. */
@ConfigurationProperties("northwind.purchasing")
public record PurchasingPolicy(BigDecimal autoApproveLimit, BigDecimal hardLimit) {
}
