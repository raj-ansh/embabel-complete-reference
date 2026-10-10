package com.northwind.support.web;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.domain.io.UserInput;
import com.northwind.support.domain.Support.Customer;
import com.northwind.support.domain.Support.CustomerReply;
import com.northwind.support.domain.Support.RefundApproval;
import com.northwind.support.domain.Support.Tier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Starts support conversations, and resumes those waiting for a support lead. */
@Service
public class SupportService {

    private static final Logger audit = LoggerFactory.getLogger("northwind.audit");

    /** A stand-in for the customer directory. */
    private static final Map<String, Tier> TIERS = Map.of("C-1001", Tier.GOLD, "C-1002", Tier.STANDARD);

    public record SupportView(String conversationId, String status, String waitingFor, CustomerReply reply) {
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    private final AgentPlatform platform;

    public SupportService(AgentPlatform platform) {
        this.platform = platform;
    }

    public SupportView ask(String customerId, String message) throws Exception {
        Agent supervisor = platform.agents().stream()
                .filter(a -> a.getName().equals("SupportSupervisor")).findFirst().orElseThrow();
        Customer customer = new Customer(customerId, TIERS.getOrDefault(customerId, Tier.STANDARD));
        ProcessOptions options = new ProcessOptions().withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.firstOf(
                EarlyTerminationPolicy.maxActions(15),               // the whole tree's work for one message
                EarlyTerminationPolicy.hardBudgetLimit(0.25)));
        AgentProcess process = platform.createAgentProcessFrom(supervisor, options, new UserInput(message), customer);
        platform.start(process).get(90, TimeUnit.SECONDS);
        audit.info("ASKED conversation={} customer={} status={}", process.getId(), customerId, process.getStatus());
        return view(process);
    }

    @SuppressWarnings("unchecked")
    public SupportView decideRefund(String conversationId, String leadId, boolean isSupportLead,
                                    boolean approved, String note) throws Exception {
        AgentProcess process = platform.getAgentProcess(conversationId);
        if (process == null || process.getStatus() != AgentProcessStatusCode.WAITING) {
            throw new ConflictException("Conversation " + conversationId + " is not waiting for a refund decision");
        }
        if (!isSupportLead) {
            audit.warn("DENIED conversation={} employee={} reason=not-support-lead", conversationId, leadId);
            throw new ForbiddenException("Only support leads can decide refunds");
        }
        FormBindingRequest<RefundApproval> pending = process.last(FormBindingRequest.class);
        pending.bind(new RefundApproval(approved, leadId, note), process);       // identity from authentication
        audit.info("REFUND-DECIDED conversation={} lead={} approved={}", conversationId, leadId, approved);
        platform.start(process).get(90, TimeUnit.SECONDS);
        return view(process);
    }

    private static SupportView view(AgentProcess process) {
        FormBindingRequest<?> pending = process.getStatus() == AgentProcessStatusCode.WAITING
                ? process.last(FormBindingRequest.class) : null;
        return new SupportView(process.getId(), process.getStatus().name(),
                pending == null ? null : pending.getPayload().getTitle(), process.last(CustomerReply.class));
    }
}
