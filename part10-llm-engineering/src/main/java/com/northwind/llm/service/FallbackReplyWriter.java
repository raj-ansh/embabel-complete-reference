package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.llm.config.ModelChoices;
import com.northwind.llm.domain.Support.Reply;
import com.northwind.llm.domain.Support.Ticket;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapter 71: an explicit fallback chain. Embabel retries a failing call itself; when the retries are
 * exhausted the exception reaches us, and we try the next role in {@code northwind.models.reply-fallbacks}.
 */
@Service
public class FallbackReplyWriter {

    public record Drafted(Reply reply, String servedByRole, List<String> failures) {
    }

    private final Ai ai;
    private final ModelChoices models;

    public FallbackReplyWriter(Ai ai, ModelChoices models) {
        this.ai = ai;
        this.models = models;
    }

    public Drafted draft(Ticket ticket) {
        List<String> failures = new ArrayList<>();
        for (String role : models.replyFallbacks()) {
            try {
                Reply reply = ai.withLlm(LlmOptions.withLlmForRole(role).withTemperature(0.4))
                        .createObject("""
                                Write a reply to the customer about the ticket between the <message> tags.

                                <message>
                                %s
                                </message>""".formatted(ticket.message()), Reply.class);
                return new Drafted(reply, role, failures);
            } catch (RuntimeException e) {          // provider outage after retries, unknown role, ...
                failures.add(role + ": " + e.getMessage());
            }
        }
        throw new IllegalStateException("No model could draft a reply: " + failures);
    }
}
