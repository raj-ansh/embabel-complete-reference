package com.northwind.platform;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.northwind.platform.security.LabTokens;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 112: the capstone's production profile, observed: probes, traces without content, tenant in logs. */
@SpringBootTest(properties = "management.tracing.sampling.probability=1.0")
@AutoConfigureMockMvc
@ActiveProfiles({"lab", "prod"})
@Import(OperationsTest.CaptureSpans.class)
class OperationsTest {

    @TestConfiguration
    static class CaptureSpans {
        @Bean
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired MockMvcTester mvc;
    @Autowired LabTokens tokens;
    @Autowired InMemorySpanExporter spans;
    @Autowired SdkTracerProvider tracerProvider;

    @Test
    void probes() throws Exception {
        for (String probe : new String[] {"/actuator/health/liveness", "/actuator/health/readiness"}) {
            var result = mvc.get().uri(probe).exchange();
            System.out.println(probe + " -> " + result.getResponse().getStatus() + " " + result.getResponse().getContentAsString());
            assertThat(result).hasStatus(200);
        }
    }

    @Test
    void oneRequestObserved() {
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        spans.reset();
        try {
            mvc.post().uri("/api/requests").header("Authorization", "Bearer " + tokens.tokenFor("C-1001", "acme-outdoors", "support"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Where is my order NW-40001?\"}").exchange();
        } finally {
            root.detachAppender(appender);
        }
        tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        var finished = spans.getFinishedSpanItems();
        boolean content = finished.stream().anyMatch(s -> s.getAttributes().asMap().values().stream()
                .anyMatch(v -> String.valueOf(v).contains("Where is my order")));
        var embabelLines = appender.list.stream().filter(e -> e.getLoggerName().equals("Embabel")).toList();
        boolean tenantEverywhere = !embabelLines.isEmpty() && embabelLines.stream()
                .allMatch(e -> "acme-outdoors".equals(e.getMDCPropertyMap().get("tenant")));

        System.out.println("spans recorded: " + (finished.size() > 10) + "; customer text in spans: " + content
                + "; every Embabel log line names the tenant: " + tenantEverywhere);
        assertThat(content).isFalse();
        assertThat(tenantEverywhere).isTrue();
    }
}
