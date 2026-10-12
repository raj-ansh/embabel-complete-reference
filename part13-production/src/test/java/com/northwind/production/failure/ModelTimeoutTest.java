package com.northwind.production.failure;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.IssueService;
import com.northwind.production.service.IssueService.Status;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 94: a model call that takes too long is abandoned. Retries multiply the timeout, so the action
 * retry settings decide the worst case: with Embabel's defaults this request took over three minutes.
 */
@SpringBootTest(properties = {
        "northwind.model-timeout=300ms",
        "embabel.agent.platform.llm-operations.data-binding.max-attempts=2",
        "embabel.agent.platform.action-qos.default.max-attempts=2",
        "embabel.agent.platform.action-qos.default.backoff-millis=200",
        "embabel.agent.platform.action-qos.default.backoff-multiplier=1.1"})
@ActiveProfiles("lab")
class ModelTimeoutTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;

    @AfterEach
    void reset() {
        LabModels.scripted(mini).setLatency(Duration.ZERO);
    }

    @Test
    void slowModelTimesOut() {
        LabModels.scripted(mini).setLatency(Duration.ofSeconds(2));

        long start = System.nanoTime();
        var outcome = issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));
        Duration took = Duration.ofNanos(System.nanoTime() - start);

        System.out.println("slow model (2 s) with a 300 ms timeout: " + outcome.status()
                + ", gave up in under 2 s: " + (took.compareTo(Duration.ofSeconds(2)) < 0)
                + ", model calls: " + LabModels.scripted(mini).received().size());
        assertThat(outcome.status()).isEqualTo(Status.FAILED);
    }
}
