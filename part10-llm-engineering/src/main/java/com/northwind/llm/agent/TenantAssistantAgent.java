package com.northwind.llm.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.ActionRetryPolicy;
import com.northwind.llm.cost.BudgetGuardRail;
import com.northwind.llm.domain.Tenancy.TenantAnswer;
import com.northwind.llm.domain.Tenancy.TenantContext;
import com.northwind.llm.domain.Tenancy.TenantQuestion;

/** Chapter 80: a tenant's question, answered only while the tenant is within budget. */
@Agent(description = "Answer a question for a shop on NorthWind's SaaS platform")
public class TenantAssistantAgent {

    private final BudgetGuardRail budget;

    public TenantAssistantAgent(BudgetGuardRail budget) {
        this.budget = budget;
    }

    @AchievesGoal(description = "The tenant's question has been answered")
    @Action(description = "Answer the question",
            actionRetryPolicy = ActionRetryPolicy.FIRE_ONCE)   // a budget refusal will not change on retry
    public TenantAnswer answer(TenantQuestion question, TenantContext tenant, OperationContext context) {
        String text = context.ai()
                .withLlmByRole("balanced")
                .withGuardRails(budget)                       // checked before the call is made
                .generateText("""
                        Write a short reply to the message between the <message> tags.
                        <message>
                        %s
                        </message>""".formatted(question.text()));
        return new TenantAnswer(text);
    }
}
