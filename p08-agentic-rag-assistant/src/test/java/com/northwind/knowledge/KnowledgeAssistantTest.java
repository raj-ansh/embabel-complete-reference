package com.northwind.knowledge;

import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.core.support.LlmInteraction;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.chat.Message;
import com.northwind.knowledge.web.ChatController;
import com.northwind.knowledge.web.ChatController.ChatRequest;
import com.northwind.knowledge.web.ChatController.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** P8: an agentic RAG chatbot over two knowledge sources, tested without a live model. */
@TestPropertySource(properties = "northwind.rag.embedder=hashing")
class KnowledgeAssistantTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    ChatController chat;

    private static Tool tool(LlmInteraction interaction, String name) {
        return interaction.getTools().stream().filter(t -> t.getDefinition().getName().equals(name)).findFirst().orElseThrow();
    }

    private static String flat(String s, int max) {
        String f = s.replaceAll("\s+", " ");
        return f.length() <= max ? f : f.substring(0, max) + "…";
    }

    @Test
    void theModelGetsTwoKnowledgeSourcesAndTheirGuidance() {
        List<String> found = new ArrayList<>();
        whenGenerateText(p -> p.contains("stove"), interaction -> {
            System.out.println("tools: " + interaction.getTools().stream().map(t -> t.getDefinition().getName()).toList());
            System.out.println("prompt contributors: " + interaction.getPromptContributors().stream()
                    .map(c -> c.getClass().getSimpleName()).toList());
            // What the model would find if it searched as instructed:
            Tool manualsSearch = tool(interaction, "manuals_vectorSearch");
            // 1. The customer's words as the query
            found.add(manualsSearch.call("{\"query\": \"weak low flame cold weather\", \"topK\": 1}").toString());
            // 2. HyDE: a short hypothetical ANSWER as the query (what the TryHyDE hint asks the model to do)
            found.add(manualsSearch.call("{\"query\": \"The stove flame is weak because gas canister pressure drops in "
                    + "cold weather. Warm the canister and use a winter-blend gas canister.\", \"topK\": 1}").toString());
            // 3. Keyword search over resolved tickets
            found.add(tool(interaction, "tickets_textSearch").call("{\"query\": \"stove flame cold\", \"topK\": 1}").toString());
            System.out.println("naive query  -> " + title(found.get(0)));
            System.out.println("HyDE query   -> " + title(found.get(1)));
            System.out.println("tickets      -> " + flat(found.get(2), 160));
            return true;
        }).thenReturn("Cold gas loses pressure below 0 °C. Warm the canister in your sleeping bag and use winter-blend gas "
                + "(Summit stove manual; ticket T-4230).");

        ChatResponse response = chat.chat("C-1001", new ChatRequest(null, "My stove flame is really weak this morning, it's freezing"));

        System.out.println("reply: " + response.reply());
        assertThat(response.reply()).contains("winter-blend");
        assertThat(found.get(0)).contains("Headlamp");                      // the naive query misses
        assertThat(found.get(1)).contains("Summit Canister Stove Manual");    // the hypothetical answer hits
        assertThat(found.get(2)).contains("T-4230");
    }

    /** The document title and score of the first result in a search tool's output. */
    private static String title(String toolOutput) {
        var m = java.util.regex.Pattern.compile(" ([0-9.]+) - # Title: ([^\\n]+)").matcher(toolOutput);
        return m.find() ? m.group(2) + " (" + m.group(1) + ")" : "no results";
    }

    @Test
    @SuppressWarnings("unchecked")
    void aConversationRemembersEarlierTurns() {
        // whenCreateObject/whenGenerateText only match single-message calls, so stub the model operation directly
        List<List<Message>> calls = new ArrayList<>();
        when(llmOperations.createObject(anyList(), any(), eq(String.class), any(), any())).thenAnswer(invocation -> {
            calls.add(List.copyOf((List<Message>) invocation.getArgument(0)));
            return calls.size() == 1
                    ? "It is probably in lock mode: hold the button for 5 seconds until it flashes twice (Lumen 400 manual)."
                    : "A full charge takes 3 hours through USB-C; the indicator turns green when it's done.";
        });

        ChatResponse first = chat.chat("C-1001", new ChatRequest(null, "My Lumen headlamp won't switch on"));
        ChatResponse second = chat.chat("C-1001", new ChatRequest(first.conversationId(), "And how long does it take to charge it?"));

        System.out.println("conversation: " + first.conversationId() + " -> " + second.conversationId());
        for (int i = 0; i < calls.size(); i++) {
            System.out.println("turn " + (i + 1) + " sends the model: "
                    + calls.get(i).stream().map(m -> m.getRole() + ": " + m.getContent()).toList());
        }
        assertThat(second.conversationId()).isEqualTo(first.conversationId());
        assertThat(calls.getLast()).hasSize(3);                                    // user, assistant, user
        assertThat(second.reply()).contains("3 hours");
    }

    @Test
    void customersCannotContinueSomeoneElsesConversation() {
        whenGenerateText(p -> true).thenReturn("Pitch with the door away from the wind.");
        ChatResponse asha = chat.chat("C-1001", new ChatRequest(null, "How do I pitch the Ridgeline tent?"));

        assertThatThrownBy(() -> chat.chat("C-2002", new ChatRequest(asha.conversationId(), "What did she ask?")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("No such conversation");
    }
}
