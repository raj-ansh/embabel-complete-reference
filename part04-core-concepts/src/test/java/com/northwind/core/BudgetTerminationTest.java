package com.northwind.core;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.core.actions.WarrantyAgent.ClaimOutcome;
import com.northwind.core.actions.WarrantyAgent.WarrantyClaim;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 30: a process that needs two actions, given a budget of one. */
class BudgetTerminationTest extends EmbabelMockitoIntegrationTest {

    @Test
    void aBudgetAloneDoesNotStopTheProcess() {
        var claim = new WarrantyClaim("W-8", "TENT-2P", LocalDate.of(2026, 3, 1), false);

        AgentProcess process = AgentInvocation.builder(agentPlatform)
                .options(new ProcessOptions().withBudget(new Budget(Budget.DEFAULT_COST_LIMIT, 1, Budget.DEFAULT_TOKEN_LIMIT)))
                .build(ClaimOutcome.class)
                .run(claim);

        assertThat(process.getStatus()).isEqualTo(AgentProcessStatusCode.COMPLETED);   // 2 actions ran
    }

    @Test
    void anEarlyTerminationPolicyStopsIt() {
        var claim = new WarrantyClaim("W-9", "TENT-2P", LocalDate.of(2026, 3, 1), false);

        AgentProcess process = AgentInvocation.builder(agentPlatform)
                .options(new ProcessOptions().withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.maxActions(1)))
                .build(ClaimOutcome.class)
                .run(claim);

        System.out.println("BUDGET-STATUS " + process.getStatus() + " history=" + process.getHistory().size());
        assertThat(process.getStatus()).isEqualTo(AgentProcessStatusCode.TERMINATED);
    }
}
