package com.northwind.advanced.skills;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.skills.EmbeddingSkillSelector;
import com.embabel.agent.skills.Skills;
import com.embabel.agent.skills.support.SkillLoadException;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.advanced.lab.LabModels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chapter 113: Agent Skills, as the model sees them. */
@SpringBootTest
@ActiveProfiles("lab")
class SkillsTest {

    @Autowired SkilledAssistant assistant;
    @Autowired Skills skills;
    @Autowired EmbeddingSkillSelector selector;
    @Autowired Ai ai;
    @Autowired @Qualifier("labStandard") SpringAiLlmService standard;

    @BeforeEach
    void clear() {
        LabModels.scripted(standard).clear();
    }

    private static List<String> toolsOffered(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions options && options.getToolCallbacks() != null) {
            return options.getToolCallbacks().stream().map(t -> t.getToolDefinition().name()).toList();
        }
        return List.of();
    }

    private static String text(Prompt prompt) {
        return String.join("\n", prompt.getInstructions().stream().map(m -> String.valueOf(m.getText())).toList());
    }

    @Test
    void theModelActivatesOnlyWhatItNeeds() {
        String letter = assistant.refusalLetter("Sam",
                "Ridgeline tent delivered 41 days ago; the returns policy allows 30 days");
        var prompts = LabModels.scripted(standard).received();
        var first = prompts.getFirst();
        System.out.println("tools offered: " + toolsOffered(first));
        System.out.println("first prompt names the skills: " + text(first).contains("<name>returns-letter</name>")
                + "; contains their instructions: " + text(first).contains("Thank the customer by name"));
        var calls = prompts.getLast().getInstructions().stream()
                .filter(m -> m instanceof AssistantMessage a && a.hasToolCalls())
                .flatMap(m -> ((AssistantMessage) m).getToolCalls().stream())
                .map(c -> c.name() + " " + c.arguments()).toList();
        System.out.println("model calls: " + prompts.size() + "; tool calls in order: " + calls);
        System.out.println("reply: " + letter);
        assertThat(text(first)).doesNotContain("Thank the customer by name");
        assertThat(calls).hasSize(2);
        assertThat(letter).contains("repair or store credit");
    }

    @Test
    void withReferenceOffersEachToolTwice() {
        ai.withLlmByRole("standard").withReference(skills)
                .generateText("Write a reply to Sam, whose return was refused.");
        System.out.println("withReference(skills) offers: " + toolsOffered(LabModels.scripted(standard).received().getFirst()));
    }

    @Test
    void referenceSkillsAreChosenByEmbedding() {
        String gear = "Which tent is lightest for a solo trek? Compare the kilograms.";
        String letter = "Write a refusal letter for a late return";
        for (String question : List.of(gear, letter)) {
            System.out.println("selected for \"" + question + "\": " + selector.select(question, skills.getSkills()).stream()
                    .map(s -> s.getSkill().getName() + " (" + String.format("%.2f", s.getScore()) + ")").toList());
        }
        String answer = assistant.gearAdvice(gear);
        var prompts = LabModels.scripted(standard).received();
        System.out.println("model calls: " + prompts.size() + "; tools offered: " + toolsOffered(prompts.getFirst())
                + "; skill instructions in the prompt: " + text(prompts.getFirst()).contains("Ultralight means"));
        System.out.println("reply: " + answer);
        assertThat(answer).contains("ultralight");
    }

    @Test
    void loadingIsValidatedAndResourcesAreConfined() {
        System.out.println("read ../SKILL.md: " + skills.readResource("returns-letter", "references", "../SKILL.md"));
        System.out.println("read ../../../pom.xml: " + skills.readResource("returns-letter", "references", "../../../pom.xml"));
        System.out.println("activate an unknown skill: " + skills.activate("delete-everything"));

        assertThatThrownBy(() -> new Skills("t", "test").withLocalSkill("test-skills/broken/missing-file"))
                .isInstanceOf(SkillLoadException.class)
                .satisfies(e -> System.out.println("missing file: " + e.getMessage()));
        var lenient = new Skills("t", "test").withLocalSkills("test-skills/broken");
        System.out.println("loading the broken folder keeps: " + lenient.getSkills().stream().map(s -> s.getName()).toList());
        var scripted = new Skills("t", "test").withLocalSkills("test-skills/scripted");
        System.out.println("skill with scripts loaded: " + scripted.getSkills().size() + "; tools: "
                + scripted.tools().stream().map(t -> t.getDefinition().getName()).toList());
    }
}
