package com.northwind.support.specialists;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.northwind.support.domain.OrderBook;
import com.northwind.support.domain.RefundLedger;
import com.northwind.support.domain.Support.BillingAnswer;
import com.northwind.support.domain.Support.BillingKind;
import com.northwind.support.domain.Support.BillingQuestion;
import com.northwind.support.domain.Support.Customer;
import com.northwind.support.domain.Support.RefundProposal;

import java.math.BigDecimal;

/**
 * Specialist: invoices and refunds. Policy is code. Refunds above the limit are PROPOSED, not issued:
 * a subagent cannot wait for a person, so the supervisor asks for approval.
 */
@Agent(name = "Billing", description = "Handle invoice requests and refunds for a customer's order")
public class BillingAgent {

    public static final BigDecimal AUTO_REFUND_LIMIT = new BigDecimal("100.00");

    public enum Route { INVOICE, AUTO_REFUND, NEEDS_APPROVAL, REFUSE }

    public record BillingCheck(Route route, String orderNumber, BigDecimal amount, String reason) {
    }

    /** Every route converges here, so the agent has exactly one goal. */
    public record BillingOutcome(BillingAnswer answer) {
    }

    static final String INVOICE = "invoiceRequest";
    static final String AUTO = "autoRefund";
    static final String APPROVAL = "refundNeedsApproval";
    static final String REFUSE = "refuseBilling";

    private final OrderBook orders;
    private final RefundLedger ledger;

    public BillingAgent(OrderBook orders, RefundLedger ledger) {
        this.orders = orders;
        this.ledger = ledger;
    }

    @Action(description = "Check the request against the order and the refund policy", post = {INVOICE, AUTO, APPROVAL, REFUSE})
    public BillingCheck check(BillingQuestion question, Customer customer) {
        var order = orders.find(question.orderNumber(), customer.customerId()).orElse(null);
        if (order == null) {
            return new BillingCheck(Route.REFUSE, question.orderNumber(), BigDecimal.ZERO,
                    "No order " + question.orderNumber() + " was found on this account.");
        }
        if (question.kind() == BillingKind.INVOICE) {
            return new BillingCheck(Route.INVOICE, order.orderNumber(), order.paid(), "Invoice requested");
        }
        BigDecimal amount = question.amount() == null ? order.paid() : question.amount();
        if (amount.signum() <= 0 || amount.compareTo(order.paid()) > 0) {
            return new BillingCheck(Route.REFUSE, order.orderNumber(), amount,
                    "A refund must be between $0.01 and the $" + order.paid() + " paid for " + order.orderNumber() + ".");
        }
        return amount.compareTo(AUTO_REFUND_LIMIT) <= 0
                ? new BillingCheck(Route.AUTO_REFUND, order.orderNumber(), amount, "Within the automatic refund limit")
                : new BillingCheck(Route.NEEDS_APPROVAL, order.orderNumber(), amount, "Above the automatic refund limit of $" + AUTO_REFUND_LIMIT);
    }

    @Condition(name = INVOICE)
    public boolean invoiceRequest(BillingCheck check) { return check.route() == Route.INVOICE; }

    @Condition(name = AUTO)
    public boolean autoRefund(BillingCheck check) { return check.route() == Route.AUTO_REFUND; }

    @Condition(name = APPROVAL)
    public boolean refundNeedsApproval(BillingCheck check) { return check.route() == Route.NEEDS_APPROVAL; }

    @Condition(name = REFUSE)
    public boolean refuseBilling(BillingCheck check) { return check.route() == Route.REFUSE; }

    @Action(pre = INVOICE, description = "Send the invoice")
    public BillingOutcome sendInvoice(BillingCheck check) {
        return new BillingOutcome(new BillingAnswer("The invoice for " + check.orderNumber() + " has been e-mailed to the address on the account.", null));
    }

    @Action(pre = AUTO, description = "Issue the refund")
    public BillingOutcome refund(BillingCheck check) {
        ledger.issue(check.orderNumber(), check.amount(), "policy");
        return new BillingOutcome(new BillingAnswer("A refund of $" + check.amount() + " for " + check.orderNumber()
                + " has been issued; it reaches the card in 3-5 working days.", null));
    }

    @Action(pre = REFUSE, description = "Explain why nothing can be done")
    public BillingOutcome refuse(BillingCheck check) {
        return new BillingOutcome(new BillingAnswer(check.reason(), null));
    }

    @Action(pre = APPROVAL, description = "Propose the refund for approval")
    public BillingOutcome propose(BillingCheck check) {
        return new BillingOutcome(new BillingAnswer("A refund of $" + check.amount() + " for " + check.orderNumber() + " needs a support lead's approval.",
                new RefundProposal(check.orderNumber(), check.amount(), check.reason())));
    }

    @AchievesGoal(description = "The billing request has been handled")
    @Action(description = "Report the billing outcome")
    public BillingAnswer report(BillingOutcome outcome) {
        return outcome.answer();
    }
}
