package com.northwind.llm;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.api.validation.guardrails.GuardRailViolationException;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.llm.cost.CostLedger;
import com.northwind.llm.domain.Support.Reply;
import com.northwind.llm.domain.Support.Ticket;
import com.northwind.llm.domain.Tenancy.TenantAnswer;
import com.northwind.llm.domain.Tenancy.TenantContext;
import com.northwind.llm.domain.Tenancy.TenantQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 80: cost per tenant, a budget guardrail, a hard per-request limit, and the dashboard data. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "northwind.budgets.monthly-usd.peak-and-pine=0.0006")
class CostTest extends LabTest {

    @Autowired AgentPlatform platform;
    @Autowired CostLedger ledger;
    @Autowired MockMvcTester mvc;

    @BeforeEach
    void emptyLedger() {
        ledger.clear();
    }

    private String ask(String tenant, String user, String question) {
        try {
            var process = AgentInvocation.create(platform, TenantAnswer.class)
                    .run(new TenantQuestion(question), new TenantContext(tenant, user));
            return "%-14s %-9s answered".formatted(tenant, process.getStatus());
        } catch (Exception e) {                    // run() may throw a checked ExecutionException
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t instanceof GuardRailViolationException violation) {
                    return "%-14s %-9s %s (thrown as %s)".formatted(tenant, "BLOCKED", violation.getMessage(),
                            e.getClass().getSimpleName());
                }
            }
            throw new IllegalStateException(e);
        }
    }

    @Test
    void budgetGuardrailStopsTheNextCall() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(ask("peak-and-pine", "rory", "Do you deliver to Skye?")
                    + "  spent so far $%.6f".formatted(ledger.spentBy("peak-and-pine")));
        }
        System.out.println(ask("acme-outdoors", "li", "Do you deliver to Skye?"));
        assertThat(ledger.report().byTenant()).containsKeys("peak-and-pine", "acme-outdoors");
    }

    private void runRiskyTicket(String label, ProcessOptions options) {
        var process = AgentInvocation.create(platform, Reply.class)
                .withProcessOptions(options)
                .run(new Ticket("T-9", "C-1002", "Your stove burned my hand. I am speaking to a lawyer."));
        System.out.println(label + " -> status: " + process.getStatus() + ", cost: $%.6f".formatted(process.totalCost())
                + ", actions run: " + process.getHistory().size());
    }

    @Test
    void hardLimitPerRequest() {
        runRiskyTicket("withBudget(new Budget(0.00002, 10, 100000))  ",
                new ProcessOptions().withBudget(new Budget(0.00002, 10, 100_000)));
        runRiskyTicket("withAdditionalEarlyTerminationPolicy(hardBudgetLimit(0.00002))",
                new ProcessOptions().withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.hardBudgetLimit(0.00002)));
    }

    @Test
    void dashboardData() throws Exception {
        ask("acme-outdoors", "li", "Where is my order?");
        ask("acme-outdoors", "sam", "Do you sell tent pegs?");
        AgentInvocation.create(platform, Reply.class)
                .run(new Ticket("T-1", "C-1001", "Where is my order NW-10442?"));
        var result = mvc.get().uri("/api/costs").exchange();
        System.out.println(result.getResponse().getContentAsString());
        assertThat(result).hasStatusOk();
    }
}
