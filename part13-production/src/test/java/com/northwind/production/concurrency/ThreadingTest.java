package com.northwind.production.concurrency;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.probes.ProbeAgents;
import com.northwind.probes.ProbeAgents.Both;
import com.northwind.probes.ProbeAgents.Probe;
import com.northwind.probes.ProbeAgents.Start;
import com.northwind.probes.ProbeAgents.ThreadSeen;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.IssueService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 95: which threads run agents, and what concurrency buys. */
@SpringBootTest
@ActiveProfiles("lab")
class ThreadingTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired AgentPlatform platform;
    @Autowired IssueService issues;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;
    @Autowired @Qualifier("labStandard") SpringAiLlmService standard;

    @BeforeEach
    void deployProbes() {
        ProbeAgents.deploy(platform);
    }

    static String normalise(String thread) {
        return thread.replaceAll("\\d+$", "<n>");
    }

    @Test
    void whichThreadRunsTheActions() throws Exception {
        var invocation = AgentInvocation.create(platform, ThreadSeen.class);
        var agent = platform.agents().stream().filter(a -> a.getName().equals("ThreadProbeAgent")).findFirst().orElseThrow();

        String run = ((ThreadSeen) invocation.run(new Probe("run")).lastResult()).thread();
        String async = ((ThreadSeen) invocation.runAsync(new Probe("async")).get().lastResult()).thread();
        String direct = ((ThreadSeen) platform.runAgentFrom(agent, new ProcessOptions(),
                Map.of("it", new Probe("direct"))).lastResult()).thread();

        System.out.println("caller thread:                " + Thread.currentThread().getName());
        System.out.println("AgentInvocation.run:          " + normalise(run));
        System.out.println("AgentInvocation.runAsync:     " + normalise(async));
        System.out.println("AgentPlatform.runAgentFrom:   " + normalise(direct));
        assertThat(run).startsWith("embabel-platform-");
        assertThat(direct).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    void actionsInOneProcessRunOneAtATime() {
        long start = System.nanoTime();
        Both both = AgentInvocation.create(platform, Both.class).invoke(new Start("s-1"));
        long ms = Duration.ofNanos(System.nanoTime() - start).toMillis();

        System.out.println("two independent 400 ms actions (SIMPLE process): at least 800 ms: " + (ms >= 800)
                + ", same thread: " + both.stock().thread().equals(both.price().thread()));
        assertThat(ms).isGreaterThanOrEqualTo(800);
    }

    @Test
    void requestsRunConcurrently() {
        LabModels.scripted(mini).setLatency(Duration.ofMillis(250));
        LabModels.scripted(standard).setLatency(Duration.ofMillis(250));
        try {
            var issue = new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn");

            long start = System.nanoTime();
            IntStream.range(0, 8).forEach(i -> issues.resolve(issue));
            long sequential = Duration.ofNanos(System.nanoTime() - start).toMillis();

            start = System.nanoTime();
            List<CompletableFuture<IssueService.Outcome>> futures = IntStream.range(0, 8)
                    .mapToObj(i -> issues.resolveAsync(issue)).toList();
            futures.forEach(CompletableFuture::join);
            long concurrent = Duration.ofNanos(System.nanoTime() - start).toMillis();

            System.out.println("8 requests, two 250 ms model calls each: one after another took at least 4 s: "
                    + (sequential >= 4000) + "; all at once finished in under 2 s: " + (concurrent < 2000));
            assertThat(concurrent).isLessThan(sequential / 2);
        } finally {
            LabModels.scripted(mini).setLatency(Duration.ZERO);
            LabModels.scripted(standard).setLatency(Duration.ZERO);
        }
    }
}
