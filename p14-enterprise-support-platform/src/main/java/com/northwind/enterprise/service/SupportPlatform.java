package com.northwind.enterprise.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.api.validation.guardrails.GuardRailViolationException;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.enterprise.domain.Requests.BillingAnswer;
import com.northwind.enterprise.domain.Requests.CustomerRequest;
import com.northwind.enterprise.domain.Requests.ResearchAnswer;
import com.northwind.enterprise.domain.Requests.Route;
import com.northwind.enterprise.domain.Requests.RouteDecision;
import com.northwind.enterprise.domain.Requests.SupportAnswer;
import com.northwind.enterprise.routing.RequestRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** P14: the platform's front door. Route, run the specialist, turn every outcome into a reply. */
@Service
public class SupportPlatform {

    private static final Logger log = LoggerFactory.getLogger(SupportPlatform.class);

    public record Reply(Route route, String text) {
    }

    static final String HANDOFF = "Thanks, a member of our team will pick this up and reply within one working day.";

    private final RequestRouter router;
    private final AgentPlatform platform;

    public SupportPlatform(RequestRouter router, AgentPlatform platform) {
        this.router = router;
        this.platform = platform;
    }

    public Reply handle(CustomerRequest request) {
        try {
            RouteDecision decision = router.route(request);
            log.info("Routed to {}: {}", decision.route(), decision.reason());
            return switch (decision.route()) {
                case SUPPORT -> new Reply(Route.SUPPORT, run(SupportAnswer.class, request).text());
                case BILLING -> new Reply(Route.BILLING, run(BillingAnswer.class, request).text());
                case RESEARCH -> new Reply(Route.RESEARCH, run(ResearchAnswer.class, request).text());
                case HUMAN -> new Reply(Route.HUMAN, HANDOFF);
            };
        } catch (Exception e) {
            if (causedBy(e, GuardRailViolationException.class)) {
                return new Reply(Route.HUMAN, "For your security, please don't send card numbers. "
                        + "We never need them to help with an order.");
            }
            log.warn("Request failed", e);
            return new Reply(Route.HUMAN, HANDOFF);                      // every failure has a human fallback
        }
    }

    private <T> T run(Class<T> answerType, CustomerRequest request) {
        return AgentInvocation.create(platform, answerType).invoke(request);
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
