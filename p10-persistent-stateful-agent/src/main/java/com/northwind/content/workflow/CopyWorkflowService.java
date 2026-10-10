package com.northwind.content.workflow;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.northwind.content.domain.Copy.CopyBrief;
import com.northwind.content.domain.Copy.CopyOutcome;
import com.northwind.content.domain.Copy.EditorFeedback;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Starts copy workflows and feeds editors' decisions back in. Every lookup goes through the platform, which
 * (with persistence enabled) restores a process from its snapshot if it is not in memory, e.g. after a restart.
 */
@Service
public class CopyWorkflowService {

    public record WorkflowView(String processId, String status, String waitingFor, CopyOutcome outcome) {
    }

    private final AgentPlatform platform;

    public CopyWorkflowService(AgentPlatform platform) {
        this.platform = platform;
    }

    public WorkflowView start(CopyBrief brief) throws Exception {
        Agent agent = platform.agents().stream()
                .filter(a -> a.getName().equals("ProductCopyAgent")).findFirst().orElseThrow();
        AgentProcess process = platform.createAgentProcessFrom(agent, new ProcessOptions(), brief);
        platform.start(process).get(60, TimeUnit.SECONDS);
        return view(process);
    }

    @SuppressWarnings("unchecked")
    public WorkflowView review(String processId, String editor, boolean approved, String comments) throws Exception {
        AgentProcess process = platform.getAgentProcess(processId);
        if (process == null || process.getStatus() != AgentProcessStatusCode.WAITING) {
            throw new IllegalStateException("Workflow " + processId + " is not waiting for a review");
        }
        FormBindingRequest<EditorFeedback> pending = process.last(FormBindingRequest.class);
        pending.bind(new EditorFeedback(approved, comments, editor), process);   // editor from authentication
        platform.start(process).get(60, TimeUnit.SECONDS);
        return view(process);
    }

    public WorkflowView status(String processId) {
        AgentProcess process = platform.getAgentProcess(processId);
        if (process == null) {
            throw new IllegalStateException("No workflow " + processId);
        }
        return view(process);
    }

    private static WorkflowView view(AgentProcess process) {
        FormBindingRequest<?> pending = process.getStatus() == AgentProcessStatusCode.WAITING
                ? process.last(FormBindingRequest.class) : null;
        return new WorkflowView(process.getId(), process.getStatus().name(),
                pending == null ? null : pending.getPayload().getTitle(), process.last(CopyOutcome.class));
    }
}
