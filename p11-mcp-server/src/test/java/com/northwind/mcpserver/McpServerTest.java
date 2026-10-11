package com.northwind.mcpserver;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** P11: the server as an external MCP client sees it, over real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("lab")
class McpServerTest {

    @LocalServerPort int port;

    McpSyncClient client;

    @BeforeEach
    void connect() {
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build())
                .requestTimeout(Duration.ofSeconds(30))
                .build();
        var init = client.initialize();
        System.out.println("connected to: " + init.serverInfo().name() + " " + init.serverInfo().version()
                + " (protocol " + init.protocolVersion() + ")");
    }

    @AfterEach
    void disconnect() {
        client.closeGracefully();
    }

    private static String text(McpSchema.CallToolResult result) {
        return result.content().stream()
                .map(c -> c instanceof McpSchema.TextContent t ? t.text() : c.toString())
                .reduce("", String::concat);
    }

    @Test
    void listsToolsAndPrompts() {
        client.listTools().tools().forEach(t -> System.out.println("tool:   " + t.name() + " - " + t.description()));
        client.listPrompts().prompts().forEach(p -> System.out.println("prompt: " + p.name() + " - " + p.description()));
        assertThat(client.listTools().tools()).extracting(McpSchema.Tool::name)
                .contains("orderStatus", "productAdvice", "stockLevel", "productDetails");
    }

    @Test
    void schemaOfAnExportedGoal() {
        var tool = client.listTools().tools().stream().filter(t -> t.name().equals("orderStatus")).findFirst().orElseThrow();
        System.out.println("orderStatus input schema: " + tool.inputSchema());
    }

    @Test
    void callsAGoalWithoutAModel() {
        var result = client.callTool(new McpSchema.CallToolRequest("orderStatus",
                Map.of("orderNumber", "NW-10442")));
        System.out.println("orderStatus -> " + text(result));
        System.out.println("structured content: " + result.structuredContent() + ", isError: " + result.isError());
    }

    @Test
    void callsAGoalThatUsesAModelAndTools() {
        var result = client.callTool(new McpSchema.CallToolRequest("productAdvice",
                Map.of("trip", "Two nights in the Lake District in May, two people")));
        System.out.println("productAdvice -> " + text(result));
    }

    @Test
    void callsAPlainTool() {
        var result = client.callTool(new McpSchema.CallToolRequest("stockLevel", Map.of("sku", "STOVE-1")));
        System.out.println("stockLevel -> " + text(result));
    }
}
