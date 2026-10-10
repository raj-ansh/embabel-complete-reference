package com.northwind.multiagent.service;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.multiagent.returns.Returns.Customer;
import com.northwind.multiagent.returns.Returns.ReturnOutcome;
import com.northwind.multiagent.returns.Returns.ReturnRequest;
import com.northwind.multiagent.returns.ReturnsDeskAgent;
import com.northwind.multiagent.service.Service.EscalationTicket;
import com.northwind.multiagent.service.Service.Intent;
import com.northwind.multiagent.service.Service.ProductAdvice;
import com.northwind.multiagent.service.Service.ProductQuestion;
import com.northwind.multiagent.service.Service.ReplyDraft;
import com.northwind.multiagent.service.Service.SupportReply;
import com.northwind.multiagent.service.Service.Triage;

/**
 * Chapter 50: a supervisor that understands the request, routes it with conditions, and delegates to
 * specialist agents. The routing is typed and inspectable: a model classifies, code decides.
 */
@Agent(description = "Handle a customer service message by routing it to the right specialist")
public class CustomerServiceSupervisor {

    static final String RETURN = "isReturn";
    static final String PRODUCT = "isProductQuestion";
    static final String OTHER = "needsHuman";

    private final ReturnsDeskAgent returnsDesk;
    private final ProductAdvisorAgent productAdvisor;

    public CustomerServiceSupervisor(ReturnsDeskAgent returnsDesk, ProductAdvisorAgent productAdvisor) {
        this.returnsDesk = returnsDesk;
        this.productAdvisor = productAdvisor;
    }

    // post: triage is what establishes the routing conditions. Without it, the planner cannot see any
    // path to the goal before a Triage exists, and the process is STUCK before it starts.
    @Action(description = "Understand the customer's message", post = {RETURN, PRODUCT, OTHER})
    public Triage triage(UserInput message, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("cheap").withTemperature(0.0))
                .createObject("""
                        Classify this customer message as RETURN, PRODUCT_QUESTION or OTHER.
                        For RETURN, extract orderNumber, reason and daysSinceDelivery if stated.
                        For PRODUCT_QUESTION, restate the question. Always give a one-line summary.
                        <message>
                        %s
                        </message>""".formatted(message.getContent()), Triage.class);
    }

    // --- routing: conditions are plain code over the typed triage -------------------------------

    @Condition(name = RETURN)
    public boolean isReturn(Triage triage) {
        return triage.intent() == Intent.RETURN && triage.orderNumber() != null;
    }

    @Condition(name = PRODUCT)
    public boolean isProductQuestion(Triage triage) {
        return triage.intent() == Intent.PRODUCT_QUESTION;
    }

    @Condition(name = OTHER)
    public boolean needsHuman(Triage triage) {
        return !isReturn(triage) && !isProductQuestion(triage);   // includes a RETURN without an order number
    }

    // --- returns branch ---------------------------------------------------------------------------

    @Action(pre = RETURN, description = "Prepare a return request")
    public ReturnRequest toReturnRequest(Triage triage) {
        int days = triage.daysSinceDelivery() == null ? 0 : triage.daysSinceDelivery();
        return new ReturnRequest(triage.orderNumber(), null, triage.reason(), days);
    }

    @Action(description = "Delegate to the returns desk")
    public ReturnOutcome handleReturn(ReturnRequest request, Customer customer) {
        return RunSubagent.fromAnnotatedInstance(returnsDesk, ReturnOutcome.class);   // which itself delegates
    }

    @Action(description = "Draft a reply about the return")
    public ReplyDraft replyAboutReturn(ReturnOutcome outcome) {
        return new ReplyDraft(outcome.message(), "returns-desk");
    }

    // --- product branch ---------------------------------------------------------------------------

    @Action(pre = PRODUCT, description = "Prepare a product question")
    public ProductQuestion toProductQuestion(Triage triage) {
        return new ProductQuestion(triage.question());
    }

    @Action(description = "Delegate to the product advisor")
    public ProductAdvice handleProductQuestion(ProductQuestion question) {
        return RunSubagent.fromAnnotatedInstance(productAdvisor, ProductAdvice.class);
    }

    @Action(description = "Draft a reply with product advice")
    public ReplyDraft replyAboutProducts(ProductAdvice advice) {
        return new ReplyDraft(advice.answer(), "product-advisor");
    }

    // --- everything else goes to a person -------------------------------------------------------

    @Action(pre = OTHER, description = "Escalate to a human agent")
    public EscalationTicket escalate(Triage triage) {
        return new EscalationTicket("customer-care", triage.summary());
    }

    @Action(description = "Draft a holding reply")
    public ReplyDraft replyAboutEscalation(EscalationTicket ticket) {
        return new ReplyDraft("Thanks, I've passed this to our customer care team, who will reply by e-mail today.",
                "human:" + ticket.queue());
    }

    // --- one goal for every branch ----------------------------------------------------------------

    @AchievesGoal(description = "The customer has received a reply")
    @Action(description = "Send the reply")
    public SupportReply deliver(ReplyDraft draft) {
        return new SupportReply(draft.text(), draft.handledBy());
    }
}
