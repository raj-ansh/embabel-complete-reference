package com.northwind.planning.supervisor;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.PlannerType;

import java.util.List;

/**
 * Chapter 35: an LLM-orchestrated (Supervisor) agent. The supervisor model sees each action's
 * description and type schema, decides what to call and in what order, and constructs the
 * request objects. Wrapper request types let the same actions run under GOAP in tests.
 */
@Agent(planner = PlannerType.SUPERVISOR,
        description = "Prepare a quarterly category performance report for NorthWind's buyers")
public class CategoryReportAgent {

    public record SalesDataRequest(String category, String quarter) {
    }

    public record SalesData(double revenue, double growthPercent, List<String> topSkus) {
    }

    public record ReturnsDataRequest(String category, String quarter) {
    }

    public record ReturnsData(double returnRatePercent, List<String> topReasons) {
    }

    public record ReportRequest(String category, String quarter) {
    }

    public record CategoryReport(String title, List<String> findings, List<String> actions) {
    }

    @Action(description = "Get revenue, growth and best-selling SKUs for a category and quarter")
    public SalesData salesData(SalesDataRequest request) {
        return new SalesData(1_240_000, 8.5, List.of("TENT-2P", "PACK-45"));     // from the data warehouse
    }

    @Action(description = "Get return rate and top return reasons for a category and quarter")
    public ReturnsData returnsData(ReturnsDataRequest request) {
        return new ReturnsData(6.1, List.of("DOES_NOT_FIT", "DAMAGED"));         // from the returns system
    }

    @AchievesGoal(description = "A category report with findings and recommended actions")
    @Action(description = "Write the category report from the gathered data")
    public CategoryReport compileReport(ReportRequest request, SalesData sales, ReturnsData returns, Ai ai) {
        return ai.withLlmByRole("balanced").createObject("""
                Write a quarterly category report for %s, %s.
                Sales: %s
                Returns: %s""".formatted(request.category(), request.quarter(), sales, returns),
                CategoryReport.class);
    }
}
