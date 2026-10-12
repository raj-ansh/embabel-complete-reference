package com.northwind.production.observability;

import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.service.IssueService;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 92: what Embabel's observability module records for one request. Spans go to an in-memory
 * exporter instead of Zipkin, Jaeger or Langfuse, so the test can print and check them.
 */
@SpringBootTest(properties = "management.tracing.sampling.probability=1.0")
@ActiveProfiles("lab")
@Import(TracingTest.CaptureSpans.class)
class TracingTest {

    /** Embabel registers a global OpenTelemetry instance; a second test context in this JVM must replace it. */
    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @TestConfiguration
    static class CaptureSpans {
        @Bean
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

    @Autowired IssueService issues;
    @Autowired InMemorySpanExporter spans;
    @Autowired MeterRegistry meters;
    @Autowired SdkTracerProvider tracerProvider;

    @Test
    void oneRequestAsATrace() {
        spans.reset();
        issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));

        tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);       // spans are exported in batches
        List<SpanData> finished = spans.getFinishedSpanItems();
        SpanData root = finished.stream()
                .filter(s -> !s.getParentSpanContext().isValid()).findFirst().orElseThrow();
        print(root, finished, 0);

        attributes(finished, "llm.invocation lab-mini");
        attributes(finished, "action triage");
        System.out.println("customer id and message in the trace: " + traceContains(finished, "C-1001") + ", "
                + traceContains(finished, "The flysheet arrived torn"));

        assertThat(finished).extracting(SpanData::getName).contains("findOrder", "llm lab-mini", "llm lab-standard")
                .noneMatch(name -> name.startsWith("http"));               // no calls to a model server we don't use
        assertThat(finished.stream().map(SpanData::getTraceId).distinct()).hasSize(1);   // one request, one trace
    }

    @Test
    void metrics() {
        issues.resolve(new IssueReport("C-1001", "NW-10442", "Wrong size sent"));

        meters.getMeters().stream()
                .map(m -> m.getId().getName())
                .filter(n -> n.startsWith("embabel."))
                .distinct().sorted()
                .forEach(n -> System.out.println("meter: " + n));
        assertThat(meters.find("embabel.llm.tokens.total").meters()).isNotEmpty();
    }

    static boolean traceContains(List<SpanData> spans, String text) {
        return spans.stream().anyMatch(s -> s.getAttributes().asMap().values().stream()
                .anyMatch(v -> String.valueOf(v).contains(text)));
    }

    static void attributes(List<SpanData> spans, String name) {
        SpanData span = spans.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
        System.out.println("attributes of \"" + name + "\":");
        Map<String, String> attributes = new TreeMap<>();
        span.getAttributes().forEach((k, v) -> attributes.put(k.getKey(), String.valueOf(v).replace("\n", " ")));
        attributes.forEach((k, v) ->
                System.out.println("  " + k + " = " + (v.length() > 60 ? v.substring(0, 60) + "..." : v)));
    }

    /** Prints the span tree: children indented under their parent, in the order they started. */
    private static void print(SpanData span, List<SpanData> all, int depth) {
        System.out.println("  ".repeat(depth) + span.getName());
        all.stream()
                .filter(s -> s.getParentSpanId().equals(span.getSpanId()))
                .sorted(Comparator.comparingLong(SpanData::getStartEpochNanos))
                .forEach(child -> print(child, all, depth + 1));
    }
}
