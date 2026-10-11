package com.northwind.llm;

import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import com.embabel.agent.api.event.LlmInvocationEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test helper: records every LLM call Embabel reports, so tests can show which model did what. */
@Component
public class CallRecorder implements AgenticEventListener {

    public record Call(String model, int promptTokens, int completionTokens, double cost) {
        @Override
        public String toString() {
            return "%-12s %4d in %4d out  $%.6f".formatted(model, promptTokens, completionTokens, cost);
        }
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        if (event instanceof LlmInvocationEvent llm) {
            var invocation = llm.getInvocation();
            calls.add(new Call(invocation.getLlmMetadata().getName(),
                    invocation.getUsage().getPromptTokens(), invocation.getUsage().getCompletionTokens(),
                    invocation.cost()));
        }
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public void clear() {
        calls.clear();
    }
}
