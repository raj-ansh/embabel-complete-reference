package com.northwind.tools;

import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.ToolCallContext;
import com.northwind.tools.catalog.CalculatorTools;
import com.northwind.tools.customer.CustomerTools;
import com.northwind.tools.customer.OrderTools;
import com.northwind.tools.docs.FileSearchTools;
import com.northwind.tools.rules.PromotionRules;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tools are plain objects: Tool.fromInstance turns @LlmTool methods into framework-agnostic Tools
 * that we can inspect (what the model sees) and call with JSON (what the model would send).
 */
class ToolUnitTest {

    private static Tool tool(Object instance, String name) {
        return Tool.fromInstance(instance).stream()
                .filter(t -> t.getDefinition().getName().equals(name)).findFirst().orElseThrow();
    }

    /** String-returning tools give Text results; object-returning tools give WithArtifact (content + object). */
    private static String text(Tool.Result result) {
        return switch (result) {
            case Tool.Result.Text t -> t.getContent();
            case Tool.Result.WithArtifact a -> a.getContent();
            default -> result.toString();
        };
    }

    private static ToolCallContext customer(String id) {
        return ToolCallContext.of(Map.of("customerId", id));
    }

    @Test
    void calculatorIsExact() {
        Tool lineTotal = tool(new CalculatorTools(), "lineTotal");

        assertThat(lineTotal.getDefinition().getInputSchema().toJsonSchema()).contains("unitPrice", "quantity");
        assertThat(text(lineTotal.call("""
                {"unitPrice": 189.00, "quantity": 3}"""))).contains("567.00");
    }

    @Test
    void toolCallContextIsInvisibleToTheModel() {
        Tool current = tool(new CustomerTools(), "currentCustomer");

        String schema = current.getDefinition().getInputSchema().toJsonSchema();
        System.out.println("currentCustomer schema: " + schema);
        assertThat(schema).doesNotContainIgnoringCase("customerId").doesNotContainIgnoringCase("context");
        assertThat(text(current.call("{}", customer("C-1001")))).contains("Asha");
    }

    @Test
    void ordersAreOwnershipChecked() {
        Tool status = tool(new OrderTools(), "orderStatus");

        assertThat(text(status.call("""
                {"orderNumber": "NW-10442"}""", customer("C-1001")))).contains("SHIPPED");
        assertThat(text(status.call("""
                {"orderNumber": "NW-10442"}""", customer("C-1002")))).contains("No order NW-10442 was found");
    }

    @Test
    void fileToolsCannotEscapeTheirDirectory() {
        Tool read = tool(new FileSearchTools(Path.of("policies")), "readPolicy");

        assertThat(text(read.call("""
                {"fileName": "../pom.xml"}"""))).startsWith("Only policy documents");
        assertThat(text(read.call("""
                {"fileName": "returns.md"}"""))).contains("30 days");
    }

    @Test
    void promotionRulesExplainThemselves() {
        Tool rules = tool(new PromotionRules(), "evaluatePromotions");

        String result = text(rules.call("""
                {"customerTier": "GOLD", "subtotal": 210.00, "items": 2, "firstOrder": true}"""));

        assertThat(result).contains("\"discountPercent\":15", "GOLD members get 10% off", "ship free");
    }

    @Test
    void everyAnnotatedMethodBecomesATool() {
        List<String> names = Tool.fromInstance(new OrderTools()).stream()
                .map(t -> t.getDefinition().getName()).toList();

        assertThat(names).containsExactlyInAnyOrder("orderStatus", "myOrders");
    }
}
