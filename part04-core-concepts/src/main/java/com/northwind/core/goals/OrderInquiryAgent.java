package com.northwind.core.goals;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Chapter 24: one agent, two goals. Each goal needs its own request type, so the input the
 * caller supplies determines which goal is reachable and therefore which one is pursued.
 */
@Agent(description = "Answer questions about a NorthWind order")
public class OrderInquiryAgent {

    public record DeliveryQuestion(String orderNumber) {
    }

    public record InvoiceRequest(String orderNumber) {
    }

    public record OrderRecord(String number, String status, LocalDate eta, BigDecimal total, String invoiceId) {
    }

    public record DeliveryEstimate(String orderNumber, LocalDate eta, String status) {
    }

    public record InvoiceCopy(String orderNumber, String invoiceId, BigDecimal total) {
    }

    private OrderRecord find(String number) {
        return new OrderRecord(number, "SHIPPED", LocalDate.of(2026, 10, 9), new BigDecimal("189.00"), "INV-77");
    }

    @Action(description = "Load the order for a delivery question")
    public OrderRecord loadForDelivery(DeliveryQuestion question) {
        return find(question.orderNumber());
    }

    @Action(description = "Load the order for an invoice request")
    public OrderRecord loadForInvoice(InvoiceRequest request) {
        return find(request.orderNumber());
    }

    @AchievesGoal(description = "The customer knows when their order will arrive",
            tags = {"delivery", "tracking"},
            examples = {"Where is my order?", "When will NW-10442 arrive?"})
    @Action
    public DeliveryEstimate estimateDelivery(DeliveryQuestion question, OrderRecord order) {
        return new DeliveryEstimate(order.number(), order.eta(), order.status());
    }

    @AchievesGoal(description = "The customer has a copy of their invoice",
            tags = {"billing"},
            examples = {"Can you send me my invoice?"})
    @Action
    public InvoiceCopy invoice(InvoiceRequest request, OrderRecord order) {
        return new InvoiceCopy(order.number(), order.invoiceId(), order.total());
    }
}
