package com.northwind.production.concurrency;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.probes.ProbeAgents;
import com.northwind.probes.ProbeAgents.Both;
import com.northwind.probes.ProbeAgents.Start;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 95: the CONCURRENT process type runs actions whose inputs are all available in parallel. */
@SpringBootTest(properties = "embabel.agent.platform.process-type=CONCURRENT")
@ActiveProfiles("lab")
class ConcurrentProcessTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired AgentPlatform platform;

    @Test
    void independentActionsOverlap() {
        ProbeAgents.deploy(platform);

        long start = System.nanoTime();
        Both both = AgentInvocation.create(platform, Both.class).invoke(new Start("s-1"));
        long ms = Duration.ofNanos(System.nanoTime() - start).toMillis();

        System.out.println("two independent 400 ms actions (CONCURRENT process): under 800 ms: " + (ms < 800)
                + ", same thread: " + both.stock().thread().equals(both.price().thread()));
        assertThat(ms).isLessThan(800);
    }
}
