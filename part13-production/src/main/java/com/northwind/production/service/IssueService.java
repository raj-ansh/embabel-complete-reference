package com.northwind.production.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.northwind.production.agent.IssueResolutionAgent;
import com.northwind.production.domain.Issues.CustomerReply;
import com.northwind.production.domain.Issues.IssueReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Part XIII: the one place that runs the agent and turns a process's final status into an outcome that
 * callers (REST, web, events) can act on. Internal details stay in the logs (Chapters 94 and 97).
 */
@Service
public class IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueService.class);
    static final String FAILED_MESSAGE = "We couldn't resolve this automatically. A member of our team will follow up.";

    public enum Status { RESOLVED, REFUSED, FAILED }

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
            case TERMINATED -> new Outcome(Status.REFUSED, null,
                    String.valueOf(process.get(IssueResolutionAgent.REFUSAL)), process.getId());
            default -> {
                log.warn("Process {} ended {}", process.getId(), process.getStatus());
                yield new Outcome(Status.FAILED, null, FAILED_MESSAGE, process.getId());
            }
        };
    }
}
