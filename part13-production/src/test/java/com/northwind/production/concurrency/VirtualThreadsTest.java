package com.northwind.production.concurrency;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.probes.ProbeAgents;
import com.northwind.probes.ProbeAgents.Probe;
import com.northwind.probes.ProbeAgents.ThreadSeen;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 95: with Spring Boot's virtual threads switched on, Embabel follows. */
@SpringBootTest(properties = "spring.threads.virtual.enabled=true")
@ActiveProfiles("lab")
class VirtualThreadsTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired AgentPlatform platform;

    @Test
    void agentsRunOnVirtualThreads() {
        ProbeAgents.deploy(platform);
        String thread = AgentInvocation.create(platform, ThreadSeen.class).invoke(new Probe("v")).thread();
        System.out.println("with spring.threads.virtual.enabled=true, actions run on: "
                + ThreadingTest.normalise(thread));
        assertThat(thread).startsWith("embabel-virtual-");
    }
}
