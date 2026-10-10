package com.northwind.knowledge.chat;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * P8: the chatbot's behaviour. One action, triggered by every user message, that answers with agentic RAG
 * over two sources. The conversation (history) is on the blackboard; the model decides what to search.
 */
@EmbabelComponent
public class SupportChatActions {

    private final ToolishRag manuals;
    private final ToolishRag tickets;

    public SupportChatActions(@Qualifier("manuals") ToolishRag manuals, @Qualifier("tickets") ToolishRag tickets) {
        this.manuals = manuals;
        this.tickets = tickets;
    }

    @Action(canRerun = true, trigger = UserMessage.class, description = "Answer the customer's latest message")
    public void respond(Conversation conversation, ActionContext context) {
        AssistantMessage reply = context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.2))
                // tools + prompt contribution, rather than withReference: avoids duplicate tools (Chapter 57)
                .withTools(manuals.tools()).withPromptContributor(manuals)
                .withTools(tickets.tools()).withPromptContributor(tickets)
                .withSystemPrompt("""
                        You are NorthWind's gear support assistant, in a chat with a customer.
                        For how-to, safety and error questions, search the manuals. For "this happened to me" problems,
                        also search resolved tickets for what fixed it for others.
                        Answer only from what you find; mention the manual or ticket you used.
                        Safety first: never suggest anything a manual warns against.
                        If you cannot find the answer, say so and offer to pass the question to a person.""")
                .respond(conversation.getMessages());
        context.sendMessage(conversation.addMessage(reply));
    }
}
