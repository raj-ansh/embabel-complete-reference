package com.northwind.support.service;

import com.northwind.support.domain.SupportDomain.CustomerProfile;
import com.northwind.support.domain.SupportDomain.IssueAssessment;
import com.northwind.support.domain.SupportDomain.SupportRequest;
import com.northwind.support.domain.SupportDomain.SupportRequest.Category;
import com.northwind.support.domain.SupportDomain.SupportRequest.Urgency;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Gathers facts from our systems and decides routing. Deterministic and fully testable. */
@Service
public class IssueAssessor {

    private final OrderLookup orders;

    public IssueAssessor(OrderLookup orders) {
        this.orders = orders;
    }

    public IssueAssessment assess(SupportRequest request, CustomerProfile customer) {
        List<String> facts = new ArrayList<>();

        if (request.orderNumber() != null) {
            orders.findForCustomer(request.orderNumber(), customer.customerId()).ifPresentOrElse(
                    order -> facts.add("Order %s status %s, expected delivery %s, total %s"
                            .formatted(order.number(), order.status(), order.expectedDelivery(), order.total())),
                    () -> facts.add("No order " + request.orderNumber() + " was found on this customer's account"));
        }

        String reason = escalationReason(request, customer);
        return new IssueAssessment(request.category(), List.copyOf(facts), reason != null, reason);
    }

    private String escalationReason(SupportRequest request, CustomerProfile customer) {
        if (request.category() == Category.BILLING) {
            return "Billing questions are handled by the payments team";
        }
        if (request.category() == Category.COMPLAINT && request.urgency() == Urgency.HIGH) {
            return "Urgent complaint";
        }
        if (customer.tier() == CustomerProfile.Tier.GOLD && request.urgency() == Urgency.HIGH) {
            return "Gold member with an urgent issue";
        }
        if (customer.openTickets() >= 3) {
            return "Customer already has several open tickets";
        }
        return null;
    }
}
