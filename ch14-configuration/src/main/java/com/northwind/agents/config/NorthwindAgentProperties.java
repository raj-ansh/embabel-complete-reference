package com.northwind.agents.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Business settings for NorthWind's agents. Thresholds and limits live here, in code-checked
 * configuration, never inside prompts. Invalid values stop the application at startup.
 */
@Validated
@ConfigurationProperties(prefix = "northwind.agents")
public record NorthwindAgentProperties(
        @NotBlank @Email String supportEmail,
        @Min(20) @Max(400) int replyMaxWords,
        @Valid @NotNull Returns returns) {

    public record Returns(
            @NotNull @DecimalMin("0.00") BigDecimal autoApproveLimit,
            @NotBlank String currency) {
    }
}
