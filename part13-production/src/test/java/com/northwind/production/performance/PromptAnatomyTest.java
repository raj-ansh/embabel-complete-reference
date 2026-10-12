package com.northwind.production.performance;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.lab.LabModels;
import com.northwind.production.service.IssueService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Chapter 100: where the input tokens of one model call go. */
@SpringBootTest
@ActiveProfiles("lab")
class PromptAnatomyTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired IssueService issues;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;

    @Test
    void triagePrompt() {
        LabModels.scripted(mini).clear();
        issues.resolve(new IssueReport("C-1001", "NW-10442", "The flysheet arrived torn"));

        for (Message message : LabModels.scripted(mini).received().getFirst().getInstructions()) {
            System.out.println("--- " + message.getMessageType() + " message, about "
                    + (message.getText().length() + 3) / 4 + " tokens:");
            System.out.println(message.getText().strip());
        }
    }
}
