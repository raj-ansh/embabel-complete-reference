package com.northwind.springai.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Lets the model decide whether to call the order-status tool while answering. */
@Service
public class OrderAssistant {

    private final ChatClient chatClient;
    private final OrderStatusTools tools;

    public OrderAssistant(ChatClient chatClient, OrderStatusTools tools) {
        this.chatClient = chatClient;
        this.tools = tools;
    }

    public String answer(String question) {
        return chatClient.prompt()
                .user(question)
                .tools(tools)            // expose the @Tool methods for this call only
                .call()
                .content();
    }
}
