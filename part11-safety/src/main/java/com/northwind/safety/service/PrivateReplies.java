package com.northwind.safety.service;

import com.embabel.agent.api.common.Ai;
import com.northwind.safety.privacy.Pseudonymiser;
import org.springframework.stereotype.Service;

/** Chapter 83: the model never sees the customer's contact details, but the reply still contains them. */
@Service
public class PrivateReplies {

    public record Exchange(String sentToModel, String fromModel, String toCustomer) {
    }

    private final Ai ai;

    public PrivateReplies(Ai ai) {
        this.ai = ai;
    }

    public Exchange reply(String customerMessage) {
        var pseudonyms = new Pseudonymiser();                 // one per conversation
        String masked = pseudonyms.mask(customerMessage);
        String fromModel = ai.withLlmByRole("balanced").generateText("""
                Write a short reply to the customer message between the <message> tags.
                Placeholders such as [EMAIL_1] stand for the customer's details; keep them unchanged.
                <message>
                %s
                </message>""".formatted(masked));
        return new Exchange(masked, fromModel, pseudonyms.restore(fromModel));
    }
}
