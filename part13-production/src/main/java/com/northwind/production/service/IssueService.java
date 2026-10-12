package com.northwind.production.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.production.agent.IssueResolutionAgent;
import com.northwind.production.domain.Issues.CustomerReply;
import com.northwind.production.domain.Issues.IssueReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Part XIII: the one place that runs the agent and turns a process's final status into an outcome that
 * callers (REST, web, events) can act on. Internal details stay in the logs (Chapters 94 and 97).
 */
@Service
public class IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueService.class);
    static final String FAILED_MESSAGE = "We couldn't resolve this automatically. A member of our team will follow up.";

    public enum Status { IN_PROGRESS, RESOLVED, REFUSED, CANCELLED, FAILED }

    public record Outcome(Status status, CustomerReply reply, String message, String processId) {
    }

    private final AgentPlatform platform;

    public IssueService(AgentPlatform platform) {
        this.platform = platform;
    }

    public Outcome resolve(IssueReport issue) {
        try {
            return outcomeOf(AgentInvocation.create(platform, CustomerReply.class).run(issue));
        } catch (Exception e) {                                   // a FAILED process surfaces as an exception
            log.warn("Issue resolution failed for order {}", issue.orderNumber(), e);
            return new Outcome(Status.FAILED, null, FAILED_MESSAGE, null);
        }
    }

    /**
     * Chapters 94 and 97: starts the agent and returns its process id at once, so that callers can poll
     * {@link #outcome(String)} or {@link #cancel(String)} the work while it runs.
     */
    public String start(IssueReport issue, ProcessOptions options) {
        AgentProcess process = platform.createAgentProcess(agent(), options, Map.of("it", issue));
        platform.start(process);
        return process.getId();
    }

    public Optional<Outcome> outcome(String processId) {
        AgentProcess process = platform.getAgentProcess(processId);
        if (process == null) {
            return Optional.empty();
        }
        return Optional.of(process.getStatus() == AgentProcessStatusCode.RUNNING
                ? new Outcome(Status.IN_PROGRESS, null, null, processId)
                : outcomeOf(process));
    }

    /** Graceful: the current action finishes, then the process stops. */
    public boolean cancel(String processId) {
        AgentProcess process = platform.getAgentProcess(processId);
        if (process == null || process.getStatus() != AgentProcessStatusCode.RUNNING) {
            return false;
        }
        process.terminateAgent("Cancelled by the customer");
        return true;
    }

    private Agent agent() {
        return platform.agents().stream()
                .filter(a -> a.getName().equals("IssueResolutionAgent"))
                .findFirst().orElseThrow();
    }

    /** Chapter 95: the same work without holding the caller's thread. */
    public CompletableFuture<Outcome> resolveAsync(IssueReport issue) {
        return AgentInvocation.create(platform, CustomerReply.class).runAsync(issue)
                .thenApply(IssueService::outcomeOf)
                .exceptionally(e -> {
                    log.warn("Issue resolution failed for order {}", issue.orderNumber(), e);
                    return new Outcome(Status.FAILED, null, FAILED_MESSAGE, null);
                });
    }

    static Outcome outcomeOf(AgentProcess process) {
        return switch (process.getStatus()) {
            case COMPLETED -> new Outcome(Status.RESOLVED, (CustomerReply) process.lastResult(), null, process.getId());
            case TERMINATED -> process.get(IssueResolutionAgent.REFUSAL) instanceof String refusal
                    ? new Outcome(Status.REFUSED, null, refusal, process.getId())
                    : new Outcome(Status.CANCELLED, null, "The request was cancelled.", process.getId());
            default -> {
                log.warn("Process {} ended {}", process.getId(), process.getStatus());
                yield new Outcome(Status.FAILED, null, FAILED_MESSAGE, process.getId());
            }
        };
    }
}
