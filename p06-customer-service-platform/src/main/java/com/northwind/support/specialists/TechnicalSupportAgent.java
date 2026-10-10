package com.northwind.support.specialists;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.support.domain.Support.TechnicalAnswer;
import com.northwind.support.domain.Support.TechnicalQuestion;

/** Specialist: gear care and repairs. A model answers, grounded in the care guides it searches. */
@Agent(name = "TechnicalSupport", description = "Answer gear care and repair questions from NorthWind's care guides")
public class TechnicalSupportAgent {

    private final CareGuideTools guides;

    public TechnicalSupportAgent(CareGuideTools guides) {
        this.guides = guides;
    }

    @AchievesGoal(description = "The technical question has been answered")
    @Action(description = "Answer from the care guides")
    public TechnicalAnswer answer(TechnicalQuestion question, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.2))
                .withToolObject(guides)
                .withSystemPrompt("""
                        You are NorthWind's gear technician. Answer ONLY from the care guides you find with the
                        searchCareGuides tool, and list the guide titles you used. If no guide covers the question,
                        say so and suggest contacting the repairs team.""")
                .createObject("""
                        <technical-question>
                        %s
                        </technical-question>""".formatted(question.question()), TechnicalAnswer.class);
    }
}
