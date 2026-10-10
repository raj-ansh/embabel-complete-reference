package com.northwind.multiagent.service;

import org.springframework.lang.Nullable;

import java.util.List;

/** The customer-service domain: what the supervisor understands, and what specialists return. */
public final class Service {

    private Service() {
    }

    public enum Intent { RETURN, PRODUCT_QUESTION, OTHER }

    /** The supervisor's reading of a message. Fields that don't apply to the intent are null. */
    public record Triage(Intent intent,
                         @Nullable String orderNumber,
                         @Nullable String reason,
                         @Nullable Integer daysSinceDelivery,
                         @Nullable String question,
                         String summary) {
    }

    public record ProductQuestion(String question) {
    }

    public record ProductAdvice(String answer, List<String> skus) {
    }

    public record EscalationTicket(String queue, String summary) {
    }

    /** Every branch produces a draft; one goal action turns it into the reply. */
    public record ReplyDraft(String text, String handledBy) {
    }

    public record SupportReply(String text, String handledBy) {
    }

    public record GiftRequest(String brief) {
    }

    public record GiftIdeas(List<String> ideas) {
    }
}
