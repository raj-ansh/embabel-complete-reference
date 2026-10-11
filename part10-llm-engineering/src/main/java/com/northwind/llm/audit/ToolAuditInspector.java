package com.northwind.llm.audit;

import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.callback.AfterToolCallContext;
import com.embabel.agent.api.tool.callback.BeforeToolCallContext;
import com.embabel.agent.api.tool.callback.ToolCallInspector;
import com.embabel.agent.core.AgentProcess;
import org.springframework.stereotype.Component;

/**
 * Chapter 79: records every tool call, its arguments, outcome and duration. Inspectors observe; they
 * cannot change what the model sees. Works in streaming and non-streaming mode alike.
 */
@Component
public class ToolAuditInspector implements ToolCallInspector {

    private final AuditTrail trail;

    public ToolAuditInspector(AuditTrail trail) {
        this.trail = trail;
    }

    @Override
    public void beforeToolCall(BeforeToolCallContext context) {
        trail.record(processId(), "tool-call", context.getToolCall().getName() + " " + context.getToolCall().getArguments());
    }

    @Override
    public void afterToolCall(AfterToolCallContext context) {
        String outcome = context.getResult() instanceof Tool.Result.Error error ? "ERROR " + error.getMessage() : "ok";
        trail.record(processId(), "tool-result", "%s %s in %d ms, %d chars".formatted(
                context.getToolCall().getName(), outcome, context.getDurationMs(), context.getResultAsString().length()));
    }

    private static String processId() {
        AgentProcess process = AgentProcess.get();          // the process running on this thread, if any
        return process == null ? "-" : process.getId().substring(0, 8);
    }
}
