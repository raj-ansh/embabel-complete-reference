package com.northwind.production.deployment;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.production.ProductionApplication;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.IssueService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.time.Duration;

/**
 * Chapter 101: what happens to a running agent when the application shuts down, as it does on every
 * Kubernetes rollout. The process is mid-way through a one-second model call when the context closes.
 */
class ShutdownTest {

    @Test
    void runningAgentAtShutdown() throws Exception {
        GlobalOpenTelemetry.resetForTest();
        var app = new SpringApplicationBuilder(ProductionApplication.class)
                .profiles("lab")
                .properties("server.port=0", "spring.main.banner-mode=off")
                .run();
        var mini = LabModels.scripted(app.getBean("labMini", SpringAiLlmService.class));
        var standard = LabModels.scripted(app.getBean("labStandard", SpringAiLlmService.class));
        mini.setLatency(Duration.ofSeconds(1));

        String id = app.getBean(IssueService.class)
                .start(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"), new ProcessOptions());
        AgentProcess process = app.getBean(AgentPlatform.class).getAgentProcess(id);
        Thread.sleep(300);                                      // triage is waiting for the model

        long start = System.nanoTime();
        app.close();
        long closeMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        Thread.sleep(1500);                                     // time for an abandoned process to finish, if it would

        System.out.println("context closed after waiting for the agent: " + (closeMs >= 500)
                + "; process status afterwards: " + process.getStatus()
                + "; reply model calls: " + standard.received().size());
        org.assertj.core.api.Assertions.assertThat(process.getStatus().name()).isEqualTo("COMPLETED");
    }
}
