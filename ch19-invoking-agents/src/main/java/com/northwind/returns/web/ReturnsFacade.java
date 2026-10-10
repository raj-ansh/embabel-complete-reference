package com.northwind.returns.web;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.northwind.returns.domain.CustomerReply;
import com.northwind.returns.domain.ReturnRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * The one place the web layer talks to the agent platform. Controllers stay thin;
 * invocation policy (result type, budget, verbosity) is decided here, in code.
 */
@Service
public class ReturnsFacade {

    private final AgentPlatform platform;

    public ReturnsFacade(AgentPlatform platform) {
        this.platform = platform;
    }

    /**
     * Per-run limits: at most $0.25 of model spend, 10 actions and 20,000 tokens.
     * The Budget records the limits; the early-termination policy is what actually stops a run.
     */
    ProcessOptions options() {
        ProcessOptions defaults = new ProcessOptions();
        return defaults
                .withBudget(new Budget(0.25, 10, 20_000))
                .withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.firstOf(
                        EarlyTerminationPolicy.maxActions(10),
                        EarlyTerminationPolicy.hardBudgetLimit(0.25),
                        EarlyTerminationPolicy.maxTokens(20_000)))
                .withVerbosity(defaults.getVerbosity().withShowPlanning(true));
    }

    private AgentInvocation<CustomerReply> invocation() {
        return AgentInvocation.builder(platform)
                .options(options())
                .build(CustomerReply.class);         // "run whichever agent produces a CustomerReply"
    }

    /** Free text from e-mail or chat. */
    public ReturnOutcome fromMessage(String message) {
        return ReturnOutcome.of(invocation().run(new UserInput(message)));
    }

    /** Structured input from the web form: the planner will skip the language step. */
    public ReturnOutcome fromForm(ReturnRequest request) {
        return ReturnOutcome.of(invocation().run(request));
    }

    /** Non-blocking variant: the caller's thread is released while the agent runs. */
    public CompletableFuture<ReturnOutcome> fromMessageAsync(String message) {
        return invocation().runAsync(new UserInput(message)).thenApply(ReturnOutcome::of);
    }

    /** What we return to clients: never the whole blackboard. */
    public record ReturnOutcome(String processId, String status, CustomerReply reply) {

        static ReturnOutcome of(AgentProcess process) {
            return new ReturnOutcome(process.getId(), process.getStatus().name(),
                    process.resultOfType(CustomerReply.class));
        }
    }
}
