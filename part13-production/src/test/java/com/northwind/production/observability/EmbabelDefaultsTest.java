package com.northwind.production.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/** Chapter 93: settings Embabel contributes to every application, which production should review. */
@SpringBootTest
@ActiveProfiles("lab")
class EmbabelDefaultsTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired Environment environment;

    @Test
    void defaultsThatReachTheApplication() {
        for (String key : List.of("logging.config", "logging.pattern.console", "spring.output.ansi.enabled",
                "management.tracing.sampling.probability", "logging.structured.format.console")) {
            System.out.println("default " + key + " = " + environment.getProperty(key, "(not set)"));
        }
    }
}
