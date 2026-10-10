package com.northwind.jokes;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.embabel.agent.test.unit.LlmInvocation;
import com.embabel.common.ai.prompt.PromptContributor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JokeServiceTest {

    private final FakeOperationContext context = FakeOperationContext.create();
    private final JokeService jokes = new JokeService(context.ai());

    private List<LlmInvocation> invocations() {
        return ((FakePromptRunner) context.promptRunner()).getLlmInvocations();
    }

    /** System prompts and few-shot examples travel as prompt contributors, alongside the user prompt. */
    private String contributions(LlmInvocation invocation) {
        return invocation.getInteraction().getPromptContributors().stream()
                .map(PromptContributor::contribution)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Test
    void quickJokeIsPlainText() {
        context.expectResponse("Why don't mountains get cold? They wear snow caps.");

        assertThat(jokes.quickJoke("mountains")).contains("snow caps");
        assertThat(invocations().getFirst().getPrompt()).contains("mountains");
    }

    @Test
    void structuredJokeUsesHighTemperatureAndHouseStyle() {
        context.expectResponse(new CampfireJoke("Why did the canoe blush?", "It saw the river bed."));

        CampfireJoke joke = jokes.structuredJoke("canoes");

        assertThat(joke.punchline()).isEqualTo("It saw the river bed.");
        var invocation = invocations().getFirst();
        assertThat(invocation.getInteraction().getLlm().getTemperature()).isEqualTo(0.9);
        assertThat(invocation.getPrompt()).isEqualTo("Write a campfire joke about canoes.");
        assertThat(contributions(invocation)).contains("family-friendly");
    }

    @Test
    void houseStyleJokeIncludesFewShotExamples() {
        context.expectResponse(new CampfireJoke("Why are campfires so popular?", "They're always lit."));

        jokes.jokeInHouseStyle("campfires");

        assertThat(contributions(invocations().getFirst()))
                .contains("too many issues with its poles")      // example 1 was sent
                .contains("where they're folding");              // example 2 was sent
    }

    @Test
    void reusedRunnerMakesTwoCalls() {
        context.expectResponse("Trail Tales Tonight");
        context.expectResponse(new CampfireJoke("Why did the hiker bring a ladder?", "To reach new heights."));

        var set = jokes.jokeSet("hiking");

        assertThat(set.title()).isEqualTo("Trail Tales Tonight");
        assertThat(invocations()).hasSize(2);
    }
}
