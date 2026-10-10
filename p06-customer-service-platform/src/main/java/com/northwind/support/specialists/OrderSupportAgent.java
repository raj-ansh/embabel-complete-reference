package com.northwind.support.specialists;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.northwind.support.domain.OrderBook;
import com.northwind.support.domain.Support.Customer;
import com.northwind.support.domain.Support.OrderAnswer;
import com.northwind.support.domain.Support.OrderQuestion;

/** Specialist: order status. Deterministic: facts come from the order system, never from a model. */
@Agent(name = "OrderSupport", description = "Report the status of a customer's order")
public class OrderSupportAgent {

    private final OrderBook orders;

    public OrderSupportAgent(OrderBook orders) {
        this.orders = orders;
    }

    @AchievesGoal(description = "The order question has been answered")
    @Action(description = "Look up the order")
    public OrderAnswer answer(OrderQuestion question, Customer customer) {
        return orders.find(question.orderNumber(), customer.customerId())
                .map(o -> new OrderAnswer("Order " + o.orderNumber() + " (" + o.item() + ") is " + o.status() + "."))
                .orElse(new OrderAnswer("No order " + question.orderNumber() + " was found on this account."));
    }
}
