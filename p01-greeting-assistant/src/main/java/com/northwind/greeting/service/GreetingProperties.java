package com.northwind.greeting.service;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Brand voice and limits for generated greetings, set per environment. */
@Validated
@ConfigurationProperties(prefix = "northwind.greeting")
public record GreetingProperties(
        @NotBlank String brandVoice,
        @Min(10) @Max(80) int maxWords,
        @DecimalMin("0.0") @DecimalMax("1.0") double temperature) {
}
