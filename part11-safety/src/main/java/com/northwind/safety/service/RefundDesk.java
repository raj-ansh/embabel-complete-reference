package com.northwind.safety.service;

import com.embabel.agent.api.common.Ai;
import com.northwind.safety.domain.Refunds.RefundOutcome;
import com.northwind.safety.domain.Refunds.RefundProposal;
import com.northwind.safety.domain.Refunds.Route;
import com.northwind.safety.guard.RefundCeilingGuardRail;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Chapter 81: the model reads the request and proposes a refund; code decides what happens to it.
 * The guardrail is the last line of defence, not the policy.
 */
@Service
public class RefundDesk {

    static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("100");

    private final Ai ai;
    private final RefundCeilingGuardRail ceiling;

    public RefundDesk(Ai ai, RefundCeilingGuardRail ceiling) {
        this.ai = ai;
        this.ceiling = ceiling;
    }

    public RefundOutcome handle(String customerMessage) {
        RefundProposal proposal = ai.withLlmByRole("balanced")
                .withGuardRails(ceiling)
                .createObject("""
                        Decide the refund for the customer request between the <message> tags.
                        <message>
                        %s
                        </message>""".formatted(customerMessage), RefundProposal.class);
        if (proposal.amount().compareTo(AUTO_APPROVE_LIMIT) <= 0) {
            return new RefundOutcome(proposal, Route.AUTO_APPROVED, "Within the automatic limit");
        }
        return new RefundOutcome(proposal, Route.NEEDS_SUPERVISOR, "Above £" + AUTO_APPROVE_LIMIT + ": queued for a supervisor (Chapter 52)");
    }
}
