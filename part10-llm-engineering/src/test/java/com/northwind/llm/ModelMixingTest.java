package com.northwind.llm;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.llm.config.ModelChoices;
import com.northwind.llm.domain.Support.Reply;
import com.northwind.llm.domain.Support.Ticket;
import com.northwind.llm.domain.Support.Triage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chapter 71: the cheap model triages every ticket; risky tickets get the strongest model. */
class ModelMixingTest extends LabTest {

    @Autowired AgentPlatform platform;
    @Autowired ModelChoices models;
    @Autowired Ai ai;

    @Test
    void choicesAreBoundFromConfiguration() {
        System.out.println("triage:          role=" + models.triage().getRole() + " temperature="
                + models.triage().getTemperature() + " maxTokens=" + models.triage().getMaxTokens());
        System.out.println("reply:           role=" + models.reply().getRole() + " temperature=" + models.reply().getTemperature());
        System.out.println("sensitive-reply: role=" + models.sensitiveReply().getRole()
                + " timeout=" + models.sensitiveReply().getTimeout());
        assertThat(models.triage().getRole()).isEqualTo("cheap");
    }

    @Test
    void routineTicket() {
        var process = AgentInvocation.create(platform, Reply.class)
                .run(new Ticket("T-1", "C-1001", "Where is my order NW-10442? It was due yesterday."));
        System.out.println("ROUTINE  " + process.resultOfType(Triage.class));
        recorder.calls().forEach(c -> System.out.println("  " + c));
        System.out.println("  reply: " + process.resultOfType(Reply.class).subject());
        assertThat(recorder.calls()).extracting(CallRecorder.Call::model).containsExactly("lab-mini", "lab-standard");
    }

    @Test
    void riskyTicket() {
        var process = AgentInvocation.create(platform, Reply.class)
                .run(new Ticket("T-2", "C-1002", "Your stove burned my hand. I am speaking to a lawyer."));
        System.out.println("RISKY    " + process.resultOfType(Triage.class));
        recorder.calls().forEach(c -> System.out.println("  " + c));
        System.out.println("  reply: " + process.resultOfType(Reply.class).subject());
        assertThat(recorder.calls()).extracting(CallRecorder.Call::model).containsExactly("lab-mini", "lab-reasoner");
    }

    @Test
    void unknownRoleFailsLoudly() {
        assertThatThrownBy(() -> ai.withLlmByRole("premium").generateText("Hello"))
                .satisfies(e -> System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage()));
    }
}
