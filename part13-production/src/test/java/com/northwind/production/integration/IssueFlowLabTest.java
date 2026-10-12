package com.northwind.production.integration;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.domain.Issues.Remedy;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.IssueService;
import com.northwind.production.service.IssueService.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 91: end to end through Embabel's whole model pipeline (prompts, structured output parsing, roles),
 * with the offline lab models standing in for real ones.
 */
@SpringBootTest
@ActiveProfiles("lab")
class IssueFlowLabTest {

    @Autowired IssueService issues;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;
    @Autowired @Qualifier("labStandard") SpringAiLlmService standard;

    @BeforeEach
    void clear() {
        LabModels.scripted(mini).clear();
        LabModels.scripted(standard).clear();
    }

    @Test
    void wrongItemIsReplaced() {
        var outcome = issues.resolve(new IssueReport("C-1001", "NW-10442", "You sent the wrong colour"));

        System.out.println("wrong item: " + outcome.reply());
        System.out.println("model calls: lab-mini " + LabModels.scripted(mini).received().size()
                + ", lab-standard " + LabModels.scripted(standard).received().size());
        assertThat(outcome.status()).isEqualTo(Status.RESOLVED);
        assertThat(outcome.reply().remedy()).isEqualTo(Remedy.REPLACE);
        assertThat(LabModels.scripted(mini).received()).hasSize(1);       // triage on the cheap model
        assertThat(LabModels.scripted(standard).received()).hasSize(1);   // the reply on the better one
    }

    @Test
    void theStructuredOutputPromptIsEmbabels() {
        issues.resolve(new IssueReport("C-1001", "NW-10442", "Arrived torn"));

        String sent = LabModels.scripted(mini).received().getFirst().getContents();
        System.out.println("triage prompt mentions a JSON schema: " + sent.contains("\"$schema\""));
        assertThat(sent).contains("<message>", "Arrived torn");
    }
}
