package com.northwind.mcpserver.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.mcpserver.security.SecureAgentTool;

import java.util.List;

/**
 * Chapter 89: an internal agent for NorthWind's finance team. Its goal is not exported, so it is not an MCP
 * tool, but A2A offers every deployed agent: without @SecureAgentTool, any A2A caller could run it.
 */
@Agent(description = "Summarise yesterday's refunds for the finance team")
@SecureAgentTool("hasAuthority('finance:read')")
public class RefundReportAgent {

    public record RefundSummary(int count, String total, List<String> orders) {
    }

    @AchievesGoal(description = "Summarise yesterday's refunds for the finance team")
    @Action(description = "Summarise refunds")
    public RefundSummary summarise(UserInput request) {
        return new RefundSummary(3, "149.97 GBP", List.of("NW-10388", "NW-10402", "NW-10417"));
    }
}
