package com.northwind.llm;

import com.embabel.agent.prompt.persona.CoStar;
import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.agent.api.common.Ai;
import com.northwind.llm.prompts.NorthwindPrompting;
import com.northwind.llm.prompts.PromotionCalendar;
import com.northwind.llm.service.BrandedReplies;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 74: what prompt contributors and a reference put into the system prompt. */
class PromptElementsTest extends LabTest {

    @Autowired BrandedReplies replies;
    @Autowired PromotionCalendar promotions;
    @Autowired NorthwindPrompting prompting;
    @Autowired Ai ai;

    private static List<String> toolNames(Prompt prompt) {
        return ((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks().stream()
                .map(t -> t.getToolDefinition().name()).toList();
    }

    @Test
    void systemPromptIsAssembledFromParts() {
        replies.reply("Can I return the tent from order NW-10442?");
        Prompt prompt = standard().received().getLast();
        System.out.println("---- system prompt ----");
        System.out.println(prompt.getSystemMessage().getText());
        System.out.println("---- tools offered ----");
        System.out.println(toolNames(prompt));
        assertThat(prompt.getSystemMessage().getText()).contains("NorthWind voice", "Nora", "Autumn sale");
    }

    @Test
    void withReferenceOffersEachToolTwice() {
        ai.withLlmByRole("balanced").withReference(prompting.returnsPolicy())
                .generateText("""
                        Write a short reply to the message between the <message> tags.
                        <message>Hi</message>""");
        System.out.println("withReference tools:   " + toolNames(standard().received().getLast()));
        replies.reply("Hi");
        System.out.println("BrandedReplies tools:  " + toolNames(standard().received().getLast()));
    }

    @Test
    void dynamicContributorIsNeverStale() {
        promotions.change("Winter sale, 15% off all stoves.");
        try {
            replies.reply("Do you have any offers?");
            String system = standard().received().getLast().getSystemMessage().getText();
            System.out.println(system.lines().filter(l -> l.startsWith("Current promotion")).findFirst().orElseThrow());
            assertThat(system).contains("Winter sale");
        } finally {
            promotions.change("Autumn sale, 20% off sleeping bags until 31 October.");
        }
    }

    @Test
    void otherReadyMadeShapes() {
        System.out.println(new RoleGoalBackstory("Senior returns specialist",
                "Decide return requests fairly and consistently", "Ten years in outdoor retail").contribution());
        System.out.println("----");
        System.out.println(new CoStar("NorthWind sells outdoor equipment online", "Answer delivery questions",
                "Short paragraphs", "Friendly", "Customers in the UK", "Plain text, under 120 words").contribution());
    }
}
