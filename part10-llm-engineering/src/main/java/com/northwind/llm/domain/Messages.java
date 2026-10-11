package com.northwind.llm.domain;

import java.util.List;

/** Chapter 75: a customer's message with the context a template needs, and the reply. */
public final class Messages {

    private Messages() {
    }

    public record CustomerMessage(String customerName, String tier, List<String> openOrders, String message) {
    }

    public record ShortReply(String text) {
    }
}
