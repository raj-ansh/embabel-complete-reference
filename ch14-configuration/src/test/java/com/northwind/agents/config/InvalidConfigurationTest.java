package com.northwind.agents.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves that bad configuration fails fast at startup instead of misbehaving at runtime. */
class InvalidConfigurationTest {

    @Configuration
    @EnableConfigurationProperties(NorthwindAgentProperties.class)
    static class PropertiesOnly {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesOnly.class);

    @Test
    void negativeApprovalLimitStopsStartup() {
        runner.withPropertyValues(
                        "northwind.agents.support-email=support@northwind.example",
                        "northwind.agents.reply-max-words=120",
                        "northwind.agents.returns.auto-approve-limit=-5",
                        "northwind.agents.returns.currency=USD")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("autoApproveLimit"));
    }
}
