package com.northwind.llm;

import com.northwind.llm.service.StockQuestions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 70: the empty-response policy nudges the small model once, and it recovers. */
@TestPropertySource(properties = {
        "embabel.agent.platform.toolloop.empty-response.max-retries=1",
        "embabel.agent.platform.toolloop.empty-response.nudge-message="
                + "You sent an empty reply. Answer the question now using the tool results."})
class SmallModelNudgeTest extends LabTest {

    @Autowired StockQuestions questions;

    @Test
    void nudgedAnswer() {
        String answer = questions.ask("Ridgeline 2-Person Tent", "TENT-2P");
        var last = mini().received().getLast();
        System.out.println("model calls: " + mini().received().size() + ", answer: \"" + answer + "\"");
        System.out.println("last message the model saw: " + last.getInstructions().getLast().getText());
        assertThat(answer).contains("14 in stock");
    }
}
