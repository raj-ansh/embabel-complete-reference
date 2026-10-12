package com.northwind.production.performance;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.IssueService;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Collectors;

/** Chapter 100: what one resolved issue costs, from Embabel's own metrics. */
@SpringBootTest
@ActiveProfiles("lab")
class CostTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;
    @Autowired MeterRegistry meters;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;

    @Test
    void costPerRequest() {
        LabModels.scripted(mini).clear();
        issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));

        for (Meter meter : meters.getMeters()) {
            String name = meter.getId().getName();
            if (name.equals("embabel.llm.tokens.total") || name.equals("embabel.llm.cost.total")
                    || name.equals("embabel.llm.requests.total")) {
                String tags = meter.getId().getTags().stream().map(t -> t.getKey() + "=" + t.getValue())
                        .sorted().collect(Collectors.joining(", "));
                double value = meter.measure().iterator().next().getValue();
                System.out.println("meter " + name + " {" + tags + "} = " + value);
            }
        }
        System.out.println("triage prompt: " + promptTokens() + " tokens");
    }

    /** The same estimate the lab model reports as usage: about four characters per token. */
    int promptTokens() {
        return (LabModels.scripted(mini).received().getFirst().getContents().length() + 3) / 4;
    }
}
