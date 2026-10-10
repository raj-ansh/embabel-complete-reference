package com.northwind.tools.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.core.CoreToolGroups;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.tools.catalog.CalculatorTools;
import com.northwind.tools.catalog.WeatherTools;
import com.northwind.tools.customer.CustomerTools;
import com.northwind.tools.customer.OrderTools;
import com.northwind.tools.data.ProductTools;
import com.northwind.tools.data.ReservationTools;
import com.northwind.tools.docs.FileSearchTools;
import com.northwind.tools.integration.EmailTools;
import com.northwind.tools.integration.TrackingTools;
import com.northwind.tools.rules.PromotionRules;
import com.northwind.tools.security.GuardedTool;

/**
 * P5: NorthWind's business assistant. One action, a tool loop, and a carefully chosen tool set:
 * read-only lookups, drafting (never sending) e-mail, and one small guarded write: a pickup hold.
 * The customer comes from tool call context.
 */
@Agent(description = "Answer a signed-in NorthWind customer's questions using catalog, order, policy and promotion tools")
public class BusinessAssistantAgent {

    public record AssistantAnswer(String answer, java.util.List<String> sources) {
    }

    private final CalculatorTools calculator;
    private final WeatherTools weather;
    private final CustomerTools customers;
    private final OrderTools orders;
    private final ProductTools products;
    private final TrackingTools tracking;
    private final EmailTools email;
    private final PromotionRules promotions;
    private final FileSearchTools policies;
    private final Tool holdForPickup;

    public BusinessAssistantAgent(CalculatorTools calculator, WeatherTools weather, CustomerTools customers,
                                  OrderTools orders, ProductTools products, TrackingTools tracking,
                                  EmailTools email, PromotionRules promotions, FileSearchTools policies,
                                  ReservationTools reservations) {
        this.calculator = calculator;
        this.weather = weather;
        this.customers = customers;
        this.orders = orders;
        this.products = products;
        this.tracking = tracking;
        this.email = email;
        this.promotions = promotions;
        this.policies = policies;
        // Chapter 47: the only write tool is wrapped: role check, per-customer call budget, audit, safe errors
        this.holdForPickup = new GuardedTool(Tool.fromInstance(reservations).getFirst(), "customer", 5);
    }

    @AchievesGoal(description = "The customer's question has been answered from NorthWind's systems")
    @Action(description = "Answer using tools")
    public AssistantAnswer answer(UserInput question, OperationContext context) {
        return context.ai()
                .withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.2))
                .withSystemPrompt("""
                        You are NorthWind Outfitters' assistant for signed-in customers.
                        Use the tools for every fact: products, prices, stock, orders, tracking, policies,
                        promotions, weather and arithmetic. Never guess a number.
                        You may DRAFT an e-mail to the customer if they ask, but you cannot send it.
                        You may place a pickup hold only when the customer explicitly asks for one.
                        If a tool says something was not found, say so plainly.
                        List the tools you used in sources.""")
                .withToolObjects(calculator, weather, customers, orders, products, tracking, email, promotions, policies)
                .withTool(holdForPickup)
                .withToolGroup(CoreToolGroups.MATH)        // a named group, resolved by the platform
                .createObject("""
                        <question>
                        %s
                        </question>""".formatted(question.getContent()), AssistantAnswer.class);
    }
}
