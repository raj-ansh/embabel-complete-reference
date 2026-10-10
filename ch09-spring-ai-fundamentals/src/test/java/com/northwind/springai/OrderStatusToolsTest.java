package com.northwind.springai;

import com.northwind.springai.tools.OrderStatusTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/** Shows what the model actually sees for a @Tool method: a name, a description and a JSON schema. */
class OrderStatusToolsTest {

    @Test
    void toolDefinitionIsGeneratedFromTheMethod() {
        ToolCallback[] callbacks = ToolCallbacks.from(new OrderStatusTools());
        ToolDefinition definition = callbacks[0].getToolDefinition();

        assertThat(definition.name()).isEqualTo("orderStatus");
        assertThat(definition.description()).contains("shipping status");
        assertThat(definition.inputSchema()).contains("orderNumber").contains("NW-10442");
        System.out.println(definition.inputSchema());
    }

    @Test
    void theApplicationExecutesTheTool() {
        ToolCallback callback = ToolCallbacks.from(new OrderStatusTools())[0];

        String result = callback.call("""
                {"orderNumber": "NW-10442"}""");

        assertThat(result).contains("SHIPPED");
    }
}
