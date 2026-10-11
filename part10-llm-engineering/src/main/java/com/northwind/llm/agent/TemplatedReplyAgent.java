package com.northwind.llm.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.api.template.TemplatedPromptRunnerBuilder;
import com.northwind.llm.domain.Messages.CustomerMessage;
import com.northwind.llm.domain.Messages.ShortReply;

import java.util.Map;

/**
 * Chapter 75: both prompts come from templates. The system prompt is rendered from
 * prompts/support/system.jinja, the user prompt from prompts/support/reply.jinja.
 */
@Agent(description = "Reply to a customer message using NorthWind's prompt templates")
public class TemplatedReplyAgent {

    @AchievesGoal(description = "The customer has a reply")
    @Action(description = "Write the reply")
    public ShortReply reply(CustomerMessage message, OperationContext context) {
        PromptRunner runner = new TemplatedPromptRunnerBuilder(context, context.ai().withLlmByRole("cheap"))
                .withSystemPromptTemplate("support/system", Map.of(
                        "brand", "NorthWind Outfitters",
                        "tier", message.tier(),
                        "language", "British English"));
        String text = runner.rendering("support/reply").generateText(Map.of(
                "customerName", message.customerName(),
                "tier", message.tier(),
                "openOrders", message.openOrders(),
                "message", message.message()));
        return new ShortReply(text);
    }
}
