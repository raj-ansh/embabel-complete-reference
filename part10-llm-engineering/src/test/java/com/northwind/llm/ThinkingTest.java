package com.northwind.llm;

import com.embabel.agent.api.common.Ai;
import com.northwind.llm.service.TentAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 78: results and reasoning, separated; and reasoning that explains a refusal. */
class ThinkingTest extends LabTest {

    @Autowired TentAdvisor advisor;
    @Autowired Ai ai;

    @Test
    void resultAndReasoning() {
        var response = advisor.recommend("Two people, under 2.5 kg");
        System.out.println("result:   " + response.getResult());
        response.getThinkingBlocks().forEach(b ->
                System.out.println("thinking: [" + b.getTagType() + " " + b.getTagValue() + "] " + b.getContent()));
        assertThat(response.getResult().sku()).isEqualTo("TENT-2P");
    }

    @Test
    void includedTagsOnly() {
        var response = advisor.recommendWithAnalysisOnly("Two people, under 2.5 kg");
        System.out.println("blocks kept: " + response.getThinkingBlocks().stream().map(b -> b.getTagValue()).toList());
        String system = reasoner().received().getLast().getSystemMessage().getText();
        System.out.println("system prompt mentions the tag: " + system.contains("analysis"));
    }

    @Test
    void reasoningExplainsAFailure() {
        var response = advisor.recommend("Two people, under $20");
        System.out.println("hasResult: " + response.hasResult());
        System.out.println("thinking:  " + response.getThinkingContent());
        assertThat(response.hasResult()).isFalse();
    }

    @Test
    void modelsWithoutThinking() {
        System.out.println("cheap supports thinking: " + ai.withLlmByRole("cheap").supportsThinking()
                + ", best supports thinking: " + ai.withLlmByRole("best").supportsThinking());
    }
}
