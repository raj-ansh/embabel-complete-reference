package com.northwind.advanced.skills;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.skills.EmbeddingSkillSelector;
import com.embabel.agent.skills.Skills;
import org.springframework.stereotype.Service;

/** Chapter 113: two ways to use skills, lazily activated by the model, or selected by embedding. */
@Service
public class SkilledAssistant {

    private final Ai ai;
    private final Skills skills;
    private final EmbeddingSkillSelector selector;

    public SkilledAssistant(Ai ai, Skills skills, EmbeddingSkillSelector selector) {
        this.ai = ai;
        this.skills = skills;
        this.selector = selector;
    }

    /** Procedural skill: the model sees each skill's name and description, and activates what it needs. */
    public String refusalLetter(String customer, String facts) {
        return ai.withLlmByRole("standard").withId("refusal-letter")
                .withTools(skills.tools())                 // activate, listResources, readResource
                .withPromptContributor(skills)             // the list of available skills
                .generateText("""
                        Write a reply to %s, whose return was refused. Use the skill that matches this task.
                        Facts: %s""".formatted(customer, facts));
    }

    /** Reference skill: chosen by similarity to the question and injected without a tool call. */
    public String gearAdvice(String question) {
        return ai.withLlmByRole("standard").withId("gear-advice")
                .withPromptContributor(selector.contributorFor(question, skills))
                .generateText(question);
    }
}
