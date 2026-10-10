package com.northwind.multiagent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.tool.Subagent;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.multiagent.service.Service.GiftIdeas;
import com.northwind.multiagent.service.Service.GiftRequest;
import com.northwind.multiagent.service.Service.ProductQuestion;

/**
 * Chapter 49: a handoff as a tool. The concierge's model decides whether, and how often, to consult the
 * product advisor; each consultation runs the advisor agent as a subprocess.
 */
@Agent(description = "Suggest gift ideas from NorthWind's range")
public class GiftConciergeAgent {

    private final ProductAdvisorAgent productAdvisor;

    public GiftConciergeAgent(ProductAdvisorAgent productAdvisor) {
        this.productAdvisor = productAdvisor;
    }

    @AchievesGoal(description = "Gift ideas have been suggested")
    @Action(description = "Suggest gifts, consulting the product advisor as needed")
    public GiftIdeas suggest(GiftRequest request, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced"))
                .withTool(Subagent.ofAnnotatedInstance(productAdvisor).consuming(ProductQuestion.class))
                .createObject("""
                        Suggest up to three gift ideas from NorthWind's range for this brief.
                        Consult the ProductAdvisor tool for anything you are unsure about.
                        <brief>
                        %s
                        </brief>""".formatted(request.brief()), GiftIdeas.class);
    }
}
