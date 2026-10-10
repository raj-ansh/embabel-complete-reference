package com.northwind.jokes;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.stereotype.Service;

/** Campfire jokes for NorthWind's social channels: a tour of the PromptRunner API. */
@Service
public class JokeService {

    static final String HOUSE_STYLE = """
            You write family-friendly campfire jokes for NorthWind Outfitters' social media.
            Jokes are about hiking, camping and the outdoors. No jokes about people's
            appearance, nationality, religion or any group. Never mention competitors.""";

    private final Ai ai;

    public JokeService(Ai ai) {
        this.ai = ai;
    }

    /** 1. The simplest call: default model, plain text out. */
    public String quickJoke(String topic) {
        return ai.withDefaultLlm()
                .generateText("Tell me one short, family-friendly camping joke about " + topic + ".");
    }

    /** 2. Explicit options and a system prompt; a typed result instead of text. */
    public CampfireJoke structuredJoke(String topic) {
        return ai.withLlm(LlmOptions.withDefaultLlm().withTemperature(0.9))   // creative task: more variety
                .withSystemPrompt(HOUSE_STYLE)
                .createObject("Write a campfire joke about " + topic + ".", CampfireJoke.class);
    }

    /** 3. A model chosen by role, and few-shot examples through the Creating API. */
    public CampfireJoke jokeInHouseStyle(String topic) {
        return ai.withLlmByRole("cheap")
                .withSystemPrompt(HOUSE_STYLE)
                .creating(CampfireJoke.class)
                .withExample("A joke about tents", new CampfireJoke(
                        "Why did the tent go to therapy?",
                        "It had too many issues with its poles."))
                .withExample("A joke about maps", new CampfireJoke(
                        "Why do maps never win at poker?",
                        "Everyone can see where they're folding."))
                .fromPrompt("Write a campfire joke about " + topic + ".");
    }

    /** 4. One configured runner reused for several prompts (same model, options and system prompt). */
    public JokeSet jokeSet(String topic) {
        PromptRunner runner = ai.withLlmByRole("cheap").withSystemPrompt(HOUSE_STYLE);
        String title = runner.generateText("Suggest a 3-word title for a set of jokes about " + topic + ".");
        CampfireJoke joke = runner.createObject("Write a campfire joke about " + topic + ".", CampfireJoke.class);
        return new JokeSet(title.strip(), joke);
    }

    /** 5. A yes/no judgement by a model, with a confidence threshold. */
    public boolean isFamilyFriendly(CampfireJoke joke) {
        return ai.withLlmByRole("cheap").evaluateCondition(
                "The joke is suitable for children and does not mock any group of people",
                joke.setup() + " " + joke.punchline(),
                0.8);
    }

    public record JokeSet(String title, CampfireJoke joke) {
    }
}
