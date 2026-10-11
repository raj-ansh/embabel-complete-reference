package com.northwind.llm;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 70: what an LlmOptions choice turns into on the wire. */
class ModelOptionsTest extends LabTest {

    @Autowired Ai ai;

    @Test
    void optionsReachTheProvider() {
        String answer = ai.withLlm(LlmOptions.withLlmForRole("cheap").withTemperature(0.0).withMaxTokens(300))
                .generateText("""
                        Write a short reply to the message between the <message> tags.
                        <message>Where is my parcel?</message>""");

        var prompt = mini().received().getLast();
        ChatOptions options = prompt.getOptions();
        System.out.println("served by:   " + recorder.calls().getFirst().model());
        System.out.println("temperature: " + options.getTemperature() + ", maxTokens: " + options.getMaxTokens());
        System.out.println("system:      " + prompt.getSystemMessage().getText().replace("\n", " | "));
        System.out.println("answer:      " + answer);

        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getMaxTokens()).isEqualTo(300);
    }

    @Test
    void knowledgeCutoffContributor() {
        String prompt = """
                Write a short reply to the message between the <message> tags.
                <message>Hello</message>""";
        ai.withLlmByRole("cheap").generateText(prompt);
        ai.withLlmByRole("balanced").generateText(prompt);
        System.out.println("lab-mini (withKnowledgeCutoffDate):  "
                + mini().received().getLast().getSystemMessage().getText().replace("\n", " | "));
        System.out.println("lab-standard (cutoff in constructor): "
                + standard().received().getLast().getSystemMessage().getText().replace("\n", " | "));
    }
}
