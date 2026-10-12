package com.northwind.production.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.service.IssueService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 93: every log line written during an agent run carries the run, agent and action in the MDC. */
@SpringBootTest(properties = "management.tracing.sampling.probability=1.0")
@ActiveProfiles("lab")
class LoggingTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;

    @Test
    void mdcCarriesTheRunAgentAndAction() {
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        try {
            issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));
        } finally {
            root.detachAppender(appender);
        }

        List<ILoggingEvent> ours = appender.list.stream()
                .filter(e -> e.getLoggerName().endsWith("IssueResolutionAgent"))
                .toList();
        ILoggingEvent event = ours.getFirst();
        var mdc = new TreeMap<>(event.getMDCPropertyMap());
        System.out.println("log line: " + event.getFormattedMessage());
        mdc.forEach((k, v) -> System.out.println("  MDC " + k + " = "
                + (k.equals("embabel.agent.run_id") ? "<run id>" : k.toLowerCase().contains("id") ? "<" + v.length() + " hex chars>" : v)));
        long withRunId = appender.list.stream().filter(e -> e.getMDCPropertyMap().containsKey("embabel.agent.run_id")).count();
        System.out.println("log lines during the run carrying a run id: " + (withRunId > 0));

        assertThat(mdc).containsEntry("embabel.action.name", "com.northwind.production.agent.IssueResolutionAgent.lookUp")
                .containsKey("embabel.agent.run_id");
    }
}
