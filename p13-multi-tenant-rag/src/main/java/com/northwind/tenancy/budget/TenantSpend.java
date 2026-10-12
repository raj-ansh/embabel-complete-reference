package com.northwind.tenancy.budget;

import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import com.embabel.agent.api.event.LlmInvocationEvent;
import com.northwind.tenancy.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chapters 106 and 107: model spend per tenant per day, from Embabel's model invocation events. Feeds the
 * daily budget check and tenant-tagged metrics. In production the totals live in a shared store.
 */
@Component
public class TenantSpend implements AgenticEventListener {

    private final Map<String, BigDecimal> spentToday = new ConcurrentHashMap<>();
    private final MeterRegistry meters;

    public TenantSpend(MeterRegistry meters) {
        this.meters = meters;
    }

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        if (!(event instanceof LlmInvocationEvent llm)) {
            return;
        }
        TenantContext tenant = llm.getAgentProcess().last(TenantContext.class);
        if (tenant == null) {
            return;
        }
        var invocation = llm.getInvocation();
        BigDecimal cost = BigDecimal.valueOf(invocation.cost());
        spentToday.merge(key(tenant.tenantId()), cost, BigDecimal::add);
        meters.counter("northwind.tenant.llm.cost", "tenant", tenant.tenantId()).increment(invocation.cost());
        meters.counter("northwind.tenant.llm.tokens", "tenant", tenant.tenantId(), "model", invocation.getLlmMetadata().getName())
                .increment(invocation.getUsage().getPromptTokens() + invocation.getUsage().getCompletionTokens());
    }

    public BigDecimal spentToday(String tenantId) {
        return spentToday.getOrDefault(key(tenantId), BigDecimal.ZERO);
    }

    private static String key(String tenantId) {
        return tenantId + "@" + LocalDate.now(ZoneOffset.UTC);
    }
}
