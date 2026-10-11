package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.reference.LlmReference;
import com.northwind.llm.prompts.NorthwindPrompting;
import org.springframework.stereotype.Service;

/** Chapter 74: the call site stays small; the shared building blocks carry the policy. */
@Service
public class BrandedReplies {

    private final Ai ai;
    private final NorthwindPrompting prompting;

    public BrandedReplies(Ai ai, NorthwindPrompting prompting) {
        this.ai = ai;
        this.prompting = prompting;
    }

    public String reply(String customerMessage) {
        LlmReference returns = prompting.returnsPolicy();
        return ai.withLlmByRole("balanced")
                .withPromptContributors(prompting.customerFacing())
                .withToolObject(returns.toolObject())       // not withReference(returns): see Chapter 74
                .withPromptContributor(returns)
                .generateText("""
                        Write a short reply to the message between the <message> tags.
                        <message>
                        %s
                        </message>""".formatted(customerMessage));
    }
}
