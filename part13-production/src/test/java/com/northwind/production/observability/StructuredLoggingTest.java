package com.northwind.production.observability;

import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.service.IssueService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 93: the prod profile's structured (JSON) logging, with Embabel's MDC keys as fields. */
@SpringBootTest
@ActiveProfiles({"lab", "prod"})
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;

    @Test
    void logLinesAreJson(CapturedOutput output) {
        issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));

        String line = output.getOut().lines().filter(l -> l.contains("Looking up order")).findFirst().orElseThrow();
        System.out.println("JSON log line: " + line
                .replaceAll("\"@timestamp\":\"[^\"]+\"", "\"@timestamp\":\"<time>\"")
                .replaceAll("\"process\":\\{\"pid\":\\d+,\"thread\":\\{\"name\":\"[^\"]*\"}}", "\"process\":{<pid, thread>}")
                .replaceAll("\"run_id\":\"[^\"]+\"", "\"run_id\":\"<run id>\""));
        assertThat(line).startsWith("{").contains("\"embabel\":{\"agent\":", "\"run_id\"");
    }
}
