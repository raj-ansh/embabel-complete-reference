package com.northwind.tenancy.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.tenancy.agent.TenantSupportAgent.SupportAnswer;
import com.northwind.tenancy.agent.TenantSupportAgent.SupportQuestion;
import com.northwind.tenancy.budget.TenantSpend;
import com.northwind.tenancy.tenant.TenantContext;
import com.northwind.tenancy.tenant.Tenants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

/** P13: runs the support agent for one tenant, within that tenant's budget. */
@Service
public class TenantSupportService {

    private static final Logger log = LoggerFactory.getLogger(TenantSupportService.class);

    public enum Status { ANSWERED, OVER_BUDGET, FAILED }

    public record Reply(Status status, String text) {
    }

    /** Chapter 106: a single request may not spend more than this, whatever the tenant's daily budget. */
    static final double PER_REQUEST_LIMIT_USD = 0.05;

    private final AgentPlatform platform;
    private final Tenants tenants;
    private final TenantSpend spend;

    public TenantSupportService(AgentPlatform platform, Tenants tenants, TenantSpend spend) {
        this.platform = platform;
        this.tenants = tenants;
        this.spend = spend;
    }

    public Reply answer(TenantContext tenant, String question) {
        if (spend.spentToday(tenant.tenantId()).compareTo(tenants.get(tenant.tenantId()).dailyBudgetUsd()) >= 0) {
            log.info("Daily model budget reached");                        // the MDC names the tenant
            return new Reply(Status.OVER_BUDGET, "The assistant is unavailable for the rest of today. "
                    + "Please contact " + tenant.tenantName() + " customer service.");
        }
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        try {
            var options = new ProcessOptions().withAdditionalEarlyTerminationPolicy(
                    EarlyTerminationPolicy.hardBudgetLimit(PER_REQUEST_LIMIT_USD));
            SupportAnswer answer = AgentInvocation.create(platform, SupportAnswer.class)
                    .withProcessOptions(options)
                    .invoke(new SupportQuestion(question), tenant);
            return new Reply(Status.ANSWERED, answer.text());
        } catch (Exception e) {
            log.warn("Support answer failed", e);
            return new Reply(Status.FAILED, "Sorry, something went wrong. Please try again later.");
        } finally {
            if (callerMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(callerMdc);                             // Chapter 93
            }
        }
    }
}
