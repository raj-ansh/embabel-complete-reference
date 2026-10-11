package com.northwind.llm;

import com.northwind.llm.domain.Support.Ticket;
import com.northwind.llm.service.FallbackReplyWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 71: when the balanced model is down, the reply comes from the next role in the chain. */
@TestPropertySource(properties = "embabel.agent.platform.llm-operations.data-binding.max-attempts=2")
class FallbackTest extends LabTest {

    @Autowired FallbackReplyWriter writer;

    @AfterEach
    void restore() {
        standard().recover();
    }

    @Test
    void fallsBackToTheCheapModel() {
        standard().failWith(new RuntimeException("503 Service Unavailable"));
        var drafted = writer.draft(new Ticket("T-3", "C-1001", "Can I change the delivery address for NW-10501?"));
        System.out.println("served by role: " + drafted.servedByRole());
        System.out.println("failures:       " + drafted.failures());
        System.out.println("calls to lab-standard: " + standard().received().size() + ", to lab-mini: " + mini().received().size());
        System.out.println("reply:          " + drafted.reply());
        assertThat(drafted.servedByRole()).isEqualTo("cheap");
    }
}
