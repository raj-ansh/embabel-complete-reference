package com.northwind.purchasing.web;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.domain.io.UserInput;
import com.northwind.purchasing.domain.Purchasing.ApprovalDecision;
import com.northwind.purchasing.domain.Purchasing.PurchaseResult;
import com.northwind.purchasing.domain.Purchasing.Requester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * The application side of human-in-the-loop: start a process, show what it waits for, and resume it
 * with a decision from an AUTHORISED person. The model never sees or produces the approver's identity.
 */
@Service
public class ApprovalService {

    private static final Logger audit = LoggerFactory.getLogger("northwind.audit");

    /** What callers see: never the raw process. */
    public record ProcessView(String processId, String status, String waitingFor, PurchaseResult result) {
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    private final AgentPlatform platform;

    public ApprovalService(AgentPlatform platform) {
        this.platform = platform;
    }

    public ProcessView submit(String text, Requester requester) throws Exception {
        Agent agent = platform.agents().stream()
                .filter(a -> a.getName().equals("PurchaseApprovalAgent")).findFirst().orElseThrow();
        ProcessOptions options = new ProcessOptions().withAdditionalEarlyTerminationPolicy(
                EarlyTerminationPolicy.firstOf(EarlyTerminationPolicy.maxActions(10), EarlyTerminationPolicy.hardBudgetLimit(0.20)));
        AgentProcess process = platform.createAgentProcessFrom(agent, options, new UserInput(text), requester);
        platform.start(process).get(60, TimeUnit.SECONDS);       // runs until it completes or WAITS
        audit.info("SUBMITTED process={} requester={} status={}", process.getId(), requester.employeeId(), process.getStatus());
        return view(process);
    }

    public ProcessView status(String processId) {
        return view(find(processId));
    }

    @SuppressWarnings("unchecked")
    public ProcessView decide(String processId, String approverId, boolean isBudgetHolder,
                              boolean approved, String comment) throws Exception {
        AgentProcess process = find(processId);
        if (process.getStatus() != AgentProcessStatusCode.WAITING) {
            throw new ConflictException("Process " + processId + " is not waiting for a decision");
        }
        FormBindingRequest<ApprovalDecision> pending = process.last(FormBindingRequest.class);
        Requester requester = process.last(Requester.class);
        if (!isBudgetHolder) {
            audit.warn("DENIED process={} approver={} reason=not-budget-holder", processId, approverId);
            throw new ForbiddenException("Only budget holders can decide purchase requests");
        }
        if (requester.employeeId().equals(approverId)) {                     // segregation of duties
            audit.warn("DENIED process={} approver={} reason=own-request", processId, approverId);
            throw new ForbiddenException("You cannot approve your own request");
        }
        // decidedBy comes from authentication, not from the request body or a model
        pending.bind(new ApprovalDecision(approved, approverId, comment), process);
        audit.info("DECIDED process={} approver={} approved={}", processId, approverId, approved);
        platform.start(process).get(60, TimeUnit.SECONDS);        // resume to the goal
        return view(process);
    }

    private AgentProcess find(String processId) {
        AgentProcess process = platform.getAgentProcess(processId);
        if (process == null) {
            throw new ConflictException("No purchase request " + processId);
        }
        return process;
    }

    private static ProcessView view(AgentProcess process) {
        FormBindingRequest<?> pending = process.getStatus() == AgentProcessStatusCode.WAITING
                ? process.last(FormBindingRequest.class) : null;
        return new ProcessView(process.getId(), process.getStatus().name(),
                pending == null ? null : pending.getPayload().getTitle(),
                process.last(PurchaseResult.class));
    }
}
