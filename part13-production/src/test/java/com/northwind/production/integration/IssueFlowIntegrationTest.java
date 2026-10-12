package com.northwind.production.integration;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.production.domain.Issues.Category;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.domain.Issues.Remedy;
import com.northwind.production.domain.Issues.Triage;
import com.northwind.production.service.IssueService;
import com.northwind.production.service.IssueService.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 91: the whole application context and the real agent platform, with only the model replaced.
 * Planning, binding, the order service and the outcome mapping all run for real.
 */
@TestPropertySource(properties = "NORTHWIND_DEFAULT_LLM=test-model")
class IssueFlowIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired IssueService issues;

    @Test
    void damagedItemIsRefundedAndTheReplyIsToldTheAmount() {
        whenCreateObject(prompt -> prompt.contains("Classify the customer's problem"), Triage.class)
                .thenReturn(new Triage(Category.DAMAGED, false));
        whenGenerateText(prompt -> prompt.contains("Write a short, friendly reply"))
                .thenReturn("Sorry about this. We have refunded 129.00 GBP.");

        var outcome = issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet is torn"));

        System.out.println("damaged: " + outcome.status() + " " + outcome.reply());
        assertThat(outcome.status()).isEqualTo(Status.RESOLVED);
        assertThat(outcome.reply().remedy()).isEqualTo(Remedy.REFUND);
        verifyCreateObject(prompt -> prompt.contains("The flysheet is torn"), Triage.class);
        verifyGenerateText(prompt -> prompt.contains("Remedy: REFUND. Amount: 129.00 GBP"));
        verifyNoMoreInteractions();
    }

    @Test
    void expensiveDamageIsEscalatedWhateverTheModelWrites() {
        whenCreateObject(prompt -> prompt.contains("Classify the customer's problem"), Triage.class)
                .thenReturn(new Triage(Category.DAMAGED, true));
        whenGenerateText(prompt -> true).thenReturn("We have refunded you in full.");   // a careless reply

        var outcome = issues.resolve(new IssueReport("C-1001", "NW-10501", "Arrived cracked"));

        System.out.println("expensive damage: " + outcome.status() + " remedy=" + outcome.reply().remedy());
        assertThat(outcome.reply().remedy()).isEqualTo(Remedy.ESCALATE);
        verifyGenerateText(prompt -> prompt.contains("Remedy: ESCALATE. Amount: 0 GBP"));
    }

    @Test
    void someoneElsesOrderNeverReachesTheModel() {
        var outcome = issues.resolve(new IssueReport("C-1001", "NW-10577", "Broken"));

        System.out.println("not your order: " + outcome.status() + " - " + outcome.message());
        assertThat(outcome.status()).isEqualTo(Status.REFUSED);
        verifyNoInteractions();
    }
}
