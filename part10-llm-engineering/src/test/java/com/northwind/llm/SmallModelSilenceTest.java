package com.northwind.llm;

import com.northwind.llm.service.StockQuestions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 70: with default settings, a small model that goes quiet after a tool call returns nothing. */
class SmallModelSilenceTest extends LabTest {

    @Autowired StockQuestions questions;

    @Test
    void blankAnswerAfterToolCall() {
        String answer = questions.ask("Ridgeline 2-Person Tent", "TENT-2P");
        System.out.println("model calls: " + mini().received().size() + ", answer: \"" + answer + "\"");
        assertThat(answer).isBlank();
    }
}
