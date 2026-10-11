package com.northwind.mcpserver;

import com.northwind.mcpserver.security.LabTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Chapter 85: what MCP looks like on the wire, as plain JSON-RPC over HTTP, without an MCP library. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("lab")
class McpWireTest {

    @LocalServerPort int port;
    @Autowired LabTokens tokens;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String json, String token, String sessionId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (sessionId != null) {
            request.header("Mcp-Session-Id", sessionId);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void show(String label, HttpResponse<String> response) {
        System.out.println(label + " -> HTTP " + response.statusCode() + " "
                + response.headers().firstValue("Content-Type").orElse("") + "\n"
                + response.body().strip().replaceAll("(?m)^id:.*$", "id:<event id>"));
    }

    @Test
    void initializeThenCallATool() throws Exception {
        String token = tokens.tokenFor("wire-demo", "orders:read");
        var init = post("""
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25",
                 "capabilities":{},"clientInfo":{"name":"wire-demo","version":"1.0"}}}""", token, null);
        show("initialize", init);
        String session = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
        System.out.println("session header present: " + !session.isBlank());

        show("notifications/initialized", post("""
                {"jsonrpc":"2.0","method":"notifications/initialized"}""", token, session));
        show("tools/call", post("""
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"orderStatus",
                 "arguments":{"orderNumber":"NW-10442"}}}""", token, session));
        show("resources/read", post("""
                {"jsonrpc":"2.0","id":3,"method":"resources/read","params":{"uri":"northwind://policies/returns"}}""",
                token, session));
    }
}
