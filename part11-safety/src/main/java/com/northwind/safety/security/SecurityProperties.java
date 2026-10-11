package com.northwind.safety.security;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Chapter 84: security settings from configuration. Secrets are never defaults in code. */
@Validated
@ConfigurationProperties("northwind.security")
public record SecurityProperties(@Positive int requestsPerMinute, String labJwtSecret) {
}
