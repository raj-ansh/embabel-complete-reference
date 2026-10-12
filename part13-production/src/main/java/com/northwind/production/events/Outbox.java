package com.northwind.production.events;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chapter 99: messages waiting to be sent to customers. In production this is a database table written in the
 * same transaction as the work, and a separate sender delivers it (the outbox pattern).
 */
@Component
public class Outbox {

    public record Message(String customerId, String orderNumber, String text) {
    }

    private final List<Message> messages = new CopyOnWriteArrayList<>();

    public void add(Message message) {
        messages.add(message);
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }

    public void clear() {
        messages.clear();
    }
}
