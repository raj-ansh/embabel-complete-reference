package com.northwind.springai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/** A ChatModel that returns a canned reply and records every prompt it receives. No network. */
public class StubChatModel implements ChatModel {

    private final String reply;
    private final List<Prompt> prompts = new ArrayList<>();

    public StubChatModel(String reply) {
        this.reply = reply;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        AssistantMessage message = AssistantMessage.builder().content(reply).build();
        return ChatResponse.builder().generations(List.of(new Generation(message))).build();
    }

    public Prompt lastPrompt() {
        return prompts.getLast();
    }
}
