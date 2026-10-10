package com.northwind.core.domain;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;

/** Chapter 26: an action that lets the model call a domain object's @Tool methods. */
@Agent(description = "Explain a NorthWind loyalty member's benefits")
public class LoyaltyAdvisorAgent {

    public record BenefitsExplanation(String text) {
    }

    @AchievesGoal(description = "The member understands their loyalty benefits")
    @Action
    public BenefitsExplanation explain(UserInput question, LoyaltyMember member, OperationContext context) {
        return context.ai()
                .withLlmByRole("cheap")
                .withToolObject(member)          // exposes only the @Tool methods, only for this call
                .createObject("""
                        Answer the member's question about their NorthWind loyalty benefits.
                        Use the tools for any discount or eligibility figures; never estimate them.
                        Member first name: %s

                        <question>
                        %s
                        </question>""".formatted(member.firstName(), question.getContent()),
                        BenefitsExplanation.class);
    }
}
