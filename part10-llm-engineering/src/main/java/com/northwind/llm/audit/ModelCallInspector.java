package com.northwind.llm.audit;

import com.embabel.agent.api.tool.callback.AfterLlmCallContext;
import com.embabel.agent.api.tool.callback.BeforeLlmCallContext;
import com.embabel.agent.api.tool.callback.ToolLoopInspector;
import com.embabel.agent.core.AgentProcess;
import org.springframework.stereotype.Component;

/** Chapter 79: one line per model round trip inside the tool loop. */
@Component
public class ModelCallInspector implements ToolLoopInspector {

    private final AuditTrail trail;

    public ModelCallInspector(AuditTrail trail) {
        this.trail = trail;
    }

    @Override
    public void beforeLlmCall(BeforeLlmCallContext context) {
        trail.record(processId(), "llm-call", "iteration %d, %d messages, %d tools".formatted(
                context.getIteration(), context.getHistory().size(), context.getTools().size()));
    }

    @Override
    public void afterLlmCall(AfterLlmCallContext context) {
        var usage = context.getUsage();
        trail.record(processId(), "llm-reply", "iteration %d, %s in / %s out".formatted(
                context.getIteration(), usage == null ? "?" : usage.getPromptTokens(), usage == null ? "?" : usage.getCompletionTokens()));
    }

    private static String processId() {
        AgentProcess process = AgentProcess.get();
        return process == null ? "-" : process.getId().substring(0, 8);
    }
}
