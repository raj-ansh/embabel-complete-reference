package com.northwind.mcpserver;

import com.northwind.mcpserver.security.LabTokens;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.catchThrowable;

/** Chapter 88: the two security layers of an Embabel MCP server, from a client's point of view. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("lab")
class McpSecurityTest {

    @LocalServerPort int port;
    @Autowired LabTokens tokens;

    private McpSyncClient client(String tokenOrNull) {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port);
        if (tokenOrNull != null) {
            transport.httpRequestCustomizer((request, method, uri, body, context) ->
                    request.header("Authorization", "Bearer " + tokenOrNull));
        }
        return McpClient.sync(transport.build()).requestTimeout(Duration.ofSeconds(30)).build();
    }

    private static String text(McpSchema.CallToolResult result) {
        return result.content().stream()
                .map(c -> c instanceof McpSchema.TextContent t ? t.text() : c.toString())
                .reduce("", String::concat);
    }

    @Test
    void noTokenNoConnection() {
        Throwable e = catchThrowable(() -> client(null).initialize());
        String message = String.valueOf(e);
        System.out.println("without a token: " + message.substring(0, Math.min(160, message.length())));
    }

    @Test
    void scopesDecideWhichAgentsRun() {
        McpSyncClient client = client(tokens.tokenFor("catalogue-widget", "products:read"));
        client.initialize();
        var orders = client.callTool(new McpSchema.CallToolRequest("orderStatus", Map.of("orderNumber", "NW-10442")));
        System.out.println("orderStatus with products:read only -> isError=" + orders.isError() + ": " + text(orders));
        var stock = client.callTool(new McpSchema.CallToolRequest("stockLevel", Map.of("sku", "TENT-2P")));
        System.out.println("stockLevel (plain tool, no @SecureAgentTool) -> " + text(stock));
        client.closeGracefully();
    }

    @Test
    void platformToolsAreOpenToEveryAuthenticatedClient() {
        McpSyncClient client = client(tokens.tokenFor("catalogue-widget", "products:read"));
        client.initialize();
        client.listTools().tools().stream().filter(t -> t.name().equals("_confirm"))
                .forEach(t -> System.out.println("_confirm schema: " + t.inputSchema()));
        var confirm = client.callTool(new McpSchema.CallToolRequest("_confirm",
                Map.of("processId", "some-other-users-process", "confirmed", true)));
        System.out.println("_confirm -> isError=" + confirm.isError() + ": " + text(confirm));
        client.closeGracefully();
    }

    @Test
    void whatAnUnauthenticatedClientIsTold() throws Exception {
        var http = HttpClient.newHttpClient();
        var mcp = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("POST /mcp without a token: HTTP " + mcp.statusCode()
                + ", WWW-Authenticate: " + mcp.headers().firstValue("WWW-Authenticate").orElse("(none)")
                .replaceAll("localhost:\\d+", "localhost:PORT"));
        var metadata = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/.well-known/oauth-protected-resource")).GET().build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("GET /.well-known/oauth-protected-resource: HTTP " + metadata.statusCode() + " "
                + metadata.body().replaceAll("localhost:\\d+", "localhost:PORT"));
    }
}
