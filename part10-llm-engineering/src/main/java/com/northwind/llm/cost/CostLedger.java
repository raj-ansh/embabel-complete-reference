package com.northwind.llm.cost;

import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import com.embabel.agent.api.event.LlmInvocationEvent;
import com.embabel.agent.core.AgentProcess;
import com.northwind.llm.domain.Tenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chapter 80: every model call, attributed to tenant, user, agent and model. Embabel emits an
 * {@link LlmInvocationEvent} after each call with its token usage and cost; this listener keeps them.
 * Production code writes to a metrics system or a table rather than to memory.
 */
@Component
public class CostLedger implements AgenticEventListener {

    public record Line(String tenant, String user, String agent, String model, String processId,
                       int promptTokens, int completionTokens, double cost) {
    }

    public record Report(BigDecimal totalUsd, int calls, long requests, Map<String, BigDecimal> byTenant,
                         Map<String, BigDecimal> byUser, Map<String, BigDecimal> byAgent, Map<String, BigDecimal> byModel) {
    }

    private final List<Line> lines = new CopyOnWriteArrayList<>();

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        if (!(event instanceof LlmInvocationEvent llm)) {
            return;
        }
        AgentProcess process = llm.getAgentProcess();
        TenantContext who = process.last(TenantContext.class);       // set by the application, if any
        var invocation = llm.getInvocation();
        lines.add(new Line(
                who == null ? "(none)" : who.tenant(),
                who == null ? "(none)" : who.userId(),
                process.getAgent().getName(),
                invocation.getLlmMetadata().getName(),
                process.getId(),
                invocation.getUsage().getPromptTokens(),
                invocation.getUsage().getCompletionTokens(),
                invocation.cost()));
    }

    public double spentBy(String tenant) {
        return lines.stream().filter(l -> l.tenant().equals(tenant)).mapToDouble(Line::cost).sum();
    }

    public Report report() {
        return new Report(
                usd(lines.stream().mapToDouble(Line::cost).sum()),
                lines.size(),
                lines.stream().map(Line::processId).distinct().count(),
                sumBy(Line::tenant), sumBy(Line::user), sumBy(Line::agent), sumBy(Line::model));
    }

    public void clear() {
        lines.clear();
    }

    private Map<String, BigDecimal> sumBy(Function<Line, String> key) {
        Map<String, BigDecimal> sums = new TreeMap<>();
        lines.stream().collect(Collectors.groupingBy(key, Collectors.summingDouble(Line::cost)))
                .forEach((k, v) -> sums.put(k, usd(v)));
        return sums;
    }

    /** Dollars to the nearest millionth: model costs per call are tiny, and doubles print badly. */
    private static BigDecimal usd(double amount) {
        return BigDecimal.valueOf(amount).setScale(6, RoundingMode.HALF_UP);
    }
}
