package com.northwind.llm;

import com.northwind.llm.audit.AuditTrail;
import com.northwind.llm.service.OrderAnswers;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 79: the audit trail of one tool-using answer, and the redaction the model saw. */
class AuditingTest extends LabTest {

    @Autowired OrderAnswers answers;
    @Autowired AuditTrail trail;

    @Test
    void auditedAndRedacted() {
        trail.clear();
        String answer = answers.answer("Where is my order NW-10442?");

        System.out.println("---- audit trail ----");
        trail.entries().forEach(System.out::println);
        String toolResultSeenByModel = standard().received().getLast().getInstructions().stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .map(m -> ((ToolResponseMessage) m).getResponses().getFirst().responseData())
                .findFirst().orElseThrow();
        System.out.println("---- tool result as the model saw it ----");
        System.out.println(toolResultSeenByModel);
        System.out.println("---- answer ----");
        System.out.println(answer);

        assertThat(toolResultSeenByModel).doesNotContain("@example.com").contains("[email removed]");
        assertThat(trail.entries()).extracting(AuditTrail.Entry::kind)
                .contains("llm-call", "tool-call", "tool-result", "llm-reply");
    }
}
