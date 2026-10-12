package com.northwind.platform.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.api.validation.guardrails.GuardRailViolationException;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.platform.budget.TenantSpend;
import com.northwind.platform.domain.Requests.BillingAnswer;
import com.northwind.platform.domain.Requests.CustomerRequest;
import com.northwind.platform.domain.Requests.ResearchAnswer;
import com.northwind.platform.domain.Requests.Route;
import com.northwind.platform.domain.Requests.RouteDecision;
import com.northwind.platform.domain.Requests.SupportAnswer;
import com.northwind.platform.routing.RequestRouter;
import com.northwind.platform.tenant.TenantContext;
import com.northwind.platform.tenant.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * P15: the front door. Check the tenant's budget, route, run the specialist for the tenant with a per-request
 * cap, and turn every outcome (including guardrail blocks and failures) into a reply.
 */
@Service
public class SupportPlatform {

    private static final Logger log = LoggerFactory.getLogger(SupportPlatform.class);

    public record Reply(Route route, String text) {
    }

    static final String HANDOFF = "Thanks, a member of our team will pick this up and reply within one working day.";
    static final double PER_REQUEST_LIMIT_USD = 0.05;

    private final RequestRouter router;
    private final AgentPlatform platform;
    private final Tenants tenants;
    private final TenantSpend spend;

    public SupportPlatform(RequestRouter router, AgentPlatform platform, Tenants tenants, TenantSpend spend) {
        this.router = router;
        this.platform = platform;
        this.tenants = tenants;
        this.spend = spend;
    }

    public Reply handle(TenantContext tenant, CustomerRequest request) {
        if (spend.spentToday(tenant.tenantId()).compareTo(tenants.get(tenant.tenantId()).dailyBudgetUsd()) >= 0) {
            return new Reply(Route.HUMAN, "Our assistant is unavailable for the rest of today. " + HANDOFF);
        }
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        try {
            RouteDecision decision = router.route(request);
            log.info("Routed to {}: {}", decision.route(), decision.reason());
            return switch (decision.route()) {
                case SUPPORT -> new Reply(Route.SUPPORT, run(SupportAnswer.class, request, tenant).text());
                case BILLING -> new Reply(Route.BILLING, run(BillingAnswer.class, request, tenant).text());
                case RESEARCH -> new Reply(Route.RESEARCH, run(ResearchAnswer.class, request, tenant).text());
                case HUMAN -> new Reply(Route.HUMAN, HANDOFF);
            };
        } catch (Exception e) {
            if (causedBy(e, GuardRailViolationException.class)) {
                return new Reply(Route.HUMAN, "For your security, please don't send card numbers. "
                        + "We never need them to help with an order.");
            }
            log.warn("Request failed", e);
            return new Reply(Route.HUMAN, HANDOFF);
        } finally {
            if (callerMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(callerMdc);                             // Chapter 93
            }
        }
    }

    private <T> T run(Class<T> answerType, CustomerRequest request, TenantContext tenant) {
        var options = new ProcessOptions().withAdditionalEarlyTerminationPolicy(
                EarlyTerminationPolicy.hardBudgetLimit(PER_REQUEST_LIMIT_USD));
        return AgentInvocation.create(platform, answerType).withProcessOptions(options).invoke(request, tenant);
    }

    private static boolean causedBy(Throwable e, Class<? extends Throwable> type) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
