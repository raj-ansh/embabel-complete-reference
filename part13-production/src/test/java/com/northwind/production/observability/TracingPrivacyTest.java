package com.northwind.production.observability;

import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.service.IssueService;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 92: the same request with message content capture switched off, as production should run. */
@SpringBootTest(properties = {
        "management.tracing.sampling.probability=1.0",
        "embabel.agent.platform.observability.capture-message-content=false"})
@ActiveProfiles("lab")
@Import(TracingTest.CaptureSpans.class)
class TracingPrivacyTest {

    /** Embabel registers a global OpenTelemetry instance; a second test context in this JVM must replace it. */
    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;
    @Autowired InMemorySpanExporter spans;
    @Autowired SdkTracerProvider tracerProvider;

    @Test
    void contentIsNotCaptured() {
        spans.reset();
        issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));
        tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        List<SpanData> finished = spans.getFinishedSpanItems();

        TracingTest.attributes(finished, "action triage");
        System.out.println("customer id and message in the trace: " + TracingTest.traceContains(finished, "C-1001") + ", "
                + TracingTest.traceContains(finished, "The flysheet arrived torn"));
        System.out.println("spans: " + finished.size() + ", token counts still recorded: "
                + finished.stream().anyMatch(s -> s.getAttributes().asMap().keySet().stream()
                        .anyMatch(k -> k.getKey().equals("gen_ai.usage.input_tokens"))));
        assertThat(TracingTest.traceContains(finished, "The flysheet arrived torn")).isFalse();
    }
}
