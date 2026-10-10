package com.northwind.support.supervisor;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.support.domain.RefundLedger;
import com.northwind.support.domain.Support.BillingAnswer;
import com.northwind.support.domain.Support.BillingQuestion;
import com.northwind.support.domain.Support.Customer;
import com.northwind.support.domain.Support.CustomerReply;
import com.northwind.support.domain.Support.Intent;
import com.northwind.support.domain.Support.OrderAnswer;
import com.northwind.support.domain.Support.OrderQuestion;
import com.northwind.support.domain.Support.RefundApproval;
import com.northwind.support.domain.Support.RefundProposal;
import com.northwind.support.domain.Support.SpecialistResult;
import com.northwind.support.domain.Support.TechnicalAnswer;
import com.northwind.support.domain.Support.TechnicalQuestion;
import com.northwind.support.domain.Support.Triage;
import com.northwind.support.specialists.BillingAgent;
import com.northwind.support.specialists.OrderSupportAgent;
import com.northwind.support.specialists.TechnicalSupportAgent;

/**
 * P6: the supervisor. A model reads the message; code routes it; specialists establish the facts;
 * a model writes the reply from those facts only.
 */
@Agent(description = "Answer a NorthWind customer's message by routing it to billing, order support or technical support")
public class SupportSupervisor {

    static final String BILLING = "billingRequest";
    static final String ORDER = "orderRequest";
    static final String TECHNICAL = "technicalRequest";
    static final String HUMAN = "needsHuman";
    static final String SETTLED = "billingSettled";
    static final String LEAD = "needsSupportLead";

    private final BillingAgent billing;
    private final OrderSupportAgent orderSupport;
    private final TechnicalSupportAgent technical;
    private final RefundLedger ledger;

    public SupportSupervisor(BillingAgent billing, OrderSupportAgent orderSupport, TechnicalSupportAgent technical,
                             RefundLedger ledger) {
        this.ledger = ledger;
        this.billing = billing;
        this.orderSupport = orderSupport;
        this.technical = technical;
    }

    // --- 1. understand ----------------------------------------------------------------------------

    @Action(description = "Understand the customer's message", post = {BILLING, ORDER, TECHNICAL, HUMAN})
    public Triage triage(UserInput message, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("cheap").withTemperature(0.0))
                .createObject("""
                        Classify this customer message:
                        BILLING (an invoice or a refund: set billingKind, orderNumber and any amount),
                        ORDER (where an order is or what state it is in: set orderNumber),
                        TECHNICAL (caring for, fixing or using gear: restate the question), or OTHER.
                        Always give a one-line summary. Never invent an order number.
                        <message>
                        %s
                        </message>""".formatted(message.getContent()), Triage.class);
    }

    // --- 2. route -----------------------------------------------------------------------------------

    @Condition(name = BILLING)
    public boolean billingRequest(Triage t) {
        return t.intent() == Intent.BILLING && t.orderNumber() != null && t.billingKind() != null;
    }

    @Condition(name = ORDER)
    public boolean orderRequest(Triage t) {
        return t.intent() == Intent.ORDER && t.orderNumber() != null;
    }

    @Condition(name = TECHNICAL)
    public boolean technicalRequest(Triage t) {
        return t.intent() == Intent.TECHNICAL && t.question() != null;
    }

    @Condition(name = HUMAN)
    public boolean needsHuman(Triage t) {
        return !billingRequest(t) && !orderRequest(t) && !technicalRequest(t);
    }

    // --- 3. delegate --------------------------------------------------------------------------------

    @Action(pre = BILLING, description = "Prepare the billing question")
    public BillingQuestion toBilling(Triage t) {
        return new BillingQuestion(t.orderNumber(), t.billingKind(), t.amount());
    }

    @Action(description = "Delegate to billing", post = {SETTLED, LEAD})
    public BillingAnswer handleBilling(BillingQuestion question, Customer customer) {
        return RunSubagent.fromAnnotatedInstance(billing, BillingAnswer.class);
    }

    @Condition(name = SETTLED)
    public boolean billingSettled(BillingAnswer answer) {
        return answer.proposal() == null;
    }

    @Condition(name = LEAD)
    public boolean needsSupportLead(BillingAnswer answer) {
        return answer.proposal() != null;
    }

    @Action(pre = SETTLED, description = "Record billing's result")
    public SpecialistResult fromBilling(BillingAnswer answer) {
        return new SpecialistResult("billing", answer.facts());
    }

    // A subagent cannot wait for a person (its parent action would fail), so approval happens HERE,
    // in the top-level process, which can pause in WAITING and be resumed by the application.
    @Action(pre = LEAD, description = "Ask a support lead to approve the refund")
    public RefundApproval askSupportLead(BillingAnswer answer, Customer customer) {
        RefundProposal p = answer.proposal();
        return WaitFor.formSubmission("Approve a refund of $%s for %s (customer %s)? %s"
                .formatted(p.amount(), p.orderNumber(), customer.customerId(), p.reason()), RefundApproval.class);
    }

    @Action(description = "Act on the support lead's decision")
    public SpecialistResult fromApproval(BillingAnswer answer, RefundApproval approval) {
        RefundProposal p = answer.proposal();
        if (!approval.approved()) {
            return new SpecialistResult("billing", "The refund for " + p.orderNumber() + " was not approved: " + approval.note());
        }
        ledger.issue(p.orderNumber(), p.amount(), approval.decidedBy());
        return new SpecialistResult("billing", "A refund of $" + p.amount() + " for " + p.orderNumber()
                + " has been approved and issued; it reaches the card in 3-5 working days.");
    }

    @Action(pre = ORDER, description = "Prepare the order question")
    public OrderQuestion toOrder(Triage t) {
        return new OrderQuestion(t.orderNumber());
    }

    @Action(description = "Delegate to order support")
    public OrderAnswer handleOrder(OrderQuestion question, Customer customer) {
        return RunSubagent.fromAnnotatedInstance(orderSupport, OrderAnswer.class);
    }

    @Action(description = "Record order support's result")
    public SpecialistResult fromOrder(OrderAnswer answer) {
        return new SpecialistResult("order-support", answer.facts());
    }

    @Action(pre = TECHNICAL, description = "Prepare the technical question")
    public TechnicalQuestion toTechnical(Triage t) {
        return new TechnicalQuestion(t.question());
    }

    @Action(description = "Delegate to technical support")
    public TechnicalAnswer handleTechnical(TechnicalQuestion question) {
        return RunSubagent.fromAnnotatedInstance(technical, TechnicalAnswer.class);
    }

    @Action(description = "Record technical support's result")
    public SpecialistResult fromTechnical(TechnicalAnswer answer) {
        return new SpecialistResult("technical-support", answer.facts());
    }

    @Action(pre = HUMAN, description = "Hand over to a person")
    public SpecialistResult escalate(Triage t) {
        return new SpecialistResult("human:customer-care",
                "This has been passed to the customer care team, who will reply by e-mail today. Summary: " + t.summary());
    }

    // --- 4. reply -----------------------------------------------------------------------------------

    @AchievesGoal(description = "The customer has received a reply")
    @Action(description = "Write the reply from the specialist's facts")
    public CustomerReply reply(UserInput message, SpecialistResult result, OperationContext context) {
        String text = context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.3))
                .generateText("""
                        Write a short, friendly reply to the customer as NorthWind Outfitters.
                        Use ONLY the facts below. Do not add promises, amounts, dates or policies that are not in them.
                        <customer-message>
                        %s
                        </customer-message>
                        <facts>
                        %s
                        </facts>""".formatted(message.getContent(), result.facts()));
        return new CustomerReply(text, result.specialist());     // who handled it is set by code, not the model
    }
}
