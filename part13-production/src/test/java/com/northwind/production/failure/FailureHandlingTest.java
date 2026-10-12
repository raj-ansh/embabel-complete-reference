package com.northwind.production.failure;

import com.embabel.agent.core.ActionException;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.InMemoryOrderService;
import com.northwind.production.service.IssueService;
import com.northwind.production.service.IssueService.Status;
import com.northwind.production.service.OrderService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 94: what happens when the order system or the model fails, and when a customer cancels. */
@SpringBootTest(properties = {
        "embabel.agent.platform.action-qos.default.max-attempts=3",      // fast retries for the test
        "embabel.agent.platform.action-qos.default.backoff-millis=50",
        "embabel.agent.platform.action-qos.default.backoff-multiplier=1.1",
        "embabel.agent.platform.llm-operations.data-binding.max-attempts=2"})
@ActiveProfiles("lab")
@Import(FailureHandlingTest.Flaky.class)
class FailureHandlingTest {

    /** The order system, with failures switched on by the test. */
    @TestConfiguration
    static class Flaky {
        static final AtomicInteger failuresLeft = new AtomicInteger();
        static final AtomicInteger calls = new AtomicInteger();

        @Bean
        @Primary
        OrderService flakyOrders(InMemoryOrderService real) {
            return orderNumber -> {
                calls.incrementAndGet();
                if (failuresLeft.getAndDecrement() > 0) {
                    throw new ActionException.Transient("order system timed out", null);
                }
                return real.find(orderNumber);
            };
        }
    }

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;
    @Autowired @Qualifier("labStandard") SpringAiLlmService standard;

    private final IssueReport damaged = new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn");

    @AfterEach
    void reset() {
        Flaky.failuresLeft.set(0);
        Flaky.calls.set(0);
        LabModels.scripted(mini).recover();
        LabModels.scripted(mini).setLatency(Duration.ZERO);
        LabModels.scripted(mini).clear();
        LabModels.scripted(standard).clear();
    }

    @Test
    void orderSystemRecoversWithinTheRetries() {
        Flaky.failuresLeft.set(2);

        var outcome = issues.resolve(damaged);

        System.out.println("order system failed twice: " + outcome.status() + " after " + Flaky.calls.get() + " calls");
        assertThat(outcome.status()).isEqualTo(Status.RESOLVED);
    }

    @Test
    void orderSystemStaysDown() {
        Flaky.failuresLeft.set(100);

        var outcome = issues.resolve(damaged);

        System.out.println("order system down: " + outcome.status() + " after " + Flaky.calls.get()
                + " calls, model calls " + LabModels.scripted(mini).received().size() + ": " + outcome.message());
        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        assertThat(LabModels.scripted(mini).received()).isEmpty();
    }

    @Test
    void modelOutage() {
        LabModels.scripted(mini).failWith(new IllegalStateException("503 Service Unavailable from the provider"));

        var outcome = issues.resolve(damaged);

        System.out.println("model outage: " + outcome.status() + " after " + LabModels.scripted(mini).received().size()
                + " model calls: " + outcome.message());
        assertThat(outcome.status()).isEqualTo(Status.FAILED);
    }

    @Test
    void customerCancelsWhileTheModelIsWorking() throws Exception {
        LabModels.scripted(mini).setLatency(Duration.ofMillis(800));

        String id = issues.start(damaged, new ProcessOptions());
        Thread.sleep(300);                                    // triage is waiting for the model
        boolean cancelled = issues.cancel(id);
        Outcome result = waitFor(id);

        System.out.println("cancel accepted: " + cancelled + ", outcome: " + result.status() + " - " + result.message()
                + ", reply model calls: " + LabModels.scripted(standard).received().size());
        assertThat(result.status()).isEqualTo(Status.CANCELLED);
        assertThat(LabModels.scripted(standard).received()).isEmpty();      // the reply was never written
    }

    private Outcome waitFor(String id) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Optional<IssueService.Outcome> outcome = issues.outcome(id);
            if (outcome.isPresent() && outcome.get().status() != Status.IN_PROGRESS) {
                return new Outcome(outcome.get().status(), outcome.get().message());
            }
            Thread.sleep(50);
        }
        throw new AssertionError("still running");
    }

    record Outcome(Status status, String message) {
    }

}
