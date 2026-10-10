package com.northwind.tools.integration;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 8: e-mail. The model may only DRAFT a message to the signed-in customer's own address.
 * Sending is a separate, human-approved step outside the tool loop (Chapter 52).
 */
public class EmailTools {

    public record EmailDraft(String draftId, String to, String subject, String body) {
    }

    private final Map<String, String> customerEmails;
    private final Map<String, EmailDraft> drafts = new ConcurrentHashMap<>();

    public EmailTools(Map<String, String> customerEmails) {
        this.customerEmails = customerEmails;
    }

    @LlmTool(description = "Draft (do not send) an e-mail to the signed-in customer; returns a draft id for review")
    public EmailDraft draftEmailToCustomer(
            @LlmTool.Param(description = "Subject line, at most 100 characters") String subject,
            @LlmTool.Param(description = "Plain-text body, at most 2000 characters") String body,
            ToolCallContext context) {
        String to = customerEmails.get(context.<String>get("customerId"));
        if (to == null) {
            throw new IllegalStateException("No e-mail address for the signed-in customer");
        }
        if (subject.length() > 100 || body.length() > 2000) {
            throw new IllegalArgumentException("Subject or body too long");
        }
        EmailDraft draft = new EmailDraft(UUID.randomUUID().toString(), to, subject, body);
        drafts.put(draft.draftId(), draft);
        return draft;
    }

    public List<EmailDraft> pendingDrafts() {
        return List.copyOf(drafts.values());
    }
}
