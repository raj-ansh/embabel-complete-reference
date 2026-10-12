package com.northwind.production.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapters 93 and 102: the prod profile replaces the defaults Embabel contributes. */
@SpringBootTest
@ActiveProfiles({"lab", "prod"})
class ProductionProfileTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired Environment environment;

    @Test
    void productionSettings() {
        for (String key : List.of("logging.config", "logging.structured.format.console", "spring.output.ansi.enabled",
                "management.tracing.sampling.probability", "embabel.agent.platform.observability.capture-message-content")) {
            System.out.println("prod " + key + " = " + environment.getProperty(key, "(not set)"));
        }
        assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("0.1");
    }
}
