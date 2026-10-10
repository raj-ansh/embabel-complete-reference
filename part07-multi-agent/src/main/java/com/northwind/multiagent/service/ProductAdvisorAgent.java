package com.northwind.multiagent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.multiagent.service.Service.ProductAdvice;
import com.northwind.multiagent.service.Service.ProductQuestion;

/** Chapter 50: a specialist with its own model role and its own narrow prompt. */
@Agent(name = "ProductAdvisor", description = "Answer a question about NorthWind's outdoor products and recommend SKUs")
public class ProductAdvisorAgent {

    @AchievesGoal(description = "The product question has been answered")
    @Action(description = "Advise on products")
    public ProductAdvice advise(ProductQuestion question, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.3))
                .withSystemPrompt("""
                        You are NorthWind's gear specialist. Recommend only from: TENT-2P Ridgeline 2-Person Tent,
                        TENT-4P Basecamp 4-Person Tent, BOOT-W9 Trailblazer Waterproof Boot, JKT-GL Glacier Jacket.
                        Be concise and practical.""")
                .createObject("""
                        <product-question>
                        %s
                        </product-question>""".formatted(question.question()), ProductAdvice.class);
    }
}
