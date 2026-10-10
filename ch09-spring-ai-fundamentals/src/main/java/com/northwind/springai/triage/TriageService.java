package com.northwind.springai.triage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Uses Spring AI's ChatClient for a single, typed model call. */
@Service
public class TriageService {

    private final ChatClient chatClient;

    public TriageService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /** Free text in, free text out. */
    public String summarise(String ticketText) {
        return chatClient.prompt()
                .system("Summarise the customer's message in one sentence of at most 25 words.")
                .user(ticketText)
                .call()
                .content();
    }

    /** Free text in, a typed Java object out (structured output). */
    public TicketTriage triage(String ticketText) {
        return chatClient.prompt()
                .system("""
                        You triage support tickets for NorthWind Outfitters.
                        Urgency is 1 (low) to 5 (critical).""")
                .user(u -> u.text("Ticket:\n<ticket>\n{ticket}\n</ticket>").param("ticket", ticketText))
                .call()
                .entity(TicketTriage.class);
    }
}
