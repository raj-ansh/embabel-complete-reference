package com.northwind.core.process;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.StuckHandler;
import com.embabel.agent.api.common.StuckHandlerResult;
import com.embabel.agent.api.common.StuckHandlingResultCode;
import com.embabel.agent.core.AgentProcess;

import java.math.BigDecimal;

/**
 * Chapter 30: a process that would get STUCK (no way to obtain a CardDesign)
 * unless the agent's StuckHandler supplies a default and asks for a replan.
 */
@Agent(description = "Issue a NorthWind gift card")
public class GiftCardAgent implements StuckHandler {

    public record GiftCardOrder(String recipient, BigDecimal amount) {
    }

    public record CardDesign(String name) {
    }

    public record GiftCard(String recipient, BigDecimal amount, String design) {
    }

    @AchievesGoal(description = "Gift card issued")
    @Action
    public GiftCard issue(GiftCardOrder order, CardDesign design) {
        return new GiftCard(order.recipient(), order.amount(), design.name());
    }

    @Override
    public StuckHandlerResult handleStuck(AgentProcess agentProcess) {
        agentProcess.addObject(new CardDesign("classic-forest"));          // supply a sensible default
        return new StuckHandlerResult("No design chosen; using the default", this,
                StuckHandlingResultCode.REPLAN, agentProcess);
    }
}
