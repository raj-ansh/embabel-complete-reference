package com.northwind.mcpserver;

import com.northwind.mcpserver.security.LabTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 89: the same Embabel goals reached over A2A: an agent card, then a JSON-RPC message. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("lab")
class A2aTest {

    @LocalServerPort int port;
    @Autowired LabTokens tokens;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    /** The interesting part of a JSON-RPC reply: the task state and its data, or the error. */
    private String summary(HttpResponse<String> response) {
        JsonNode reply = json.readTree(response.body());
        if (reply.has("error")) {
            return "error " + reply.get("error");
        }
        JsonNode task = reply.get("result");
        return task.get("kind").asString() + " " + task.at("/status/state").asString() + ": "
                + task.at("/artifacts/0/parts/0/data/output");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendMessage(String text, String tokenOrNull) throws Exception {
        String body = """
                {"jsonrpc":"2.0","id":"1","method":"message/send","params":{"message":{
                  "kind":"message","messageId":"m-1","role":"user",
                  "parts":[{"kind":"text","text":"%s"}]}}}""".formatted(text);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/a2a"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (tokenOrNull != null) {
            request.header("Authorization", "Bearer " + tokenOrNull);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void agentCard() throws Exception {
        var response = get("/a2a/.well-known/agent.json");
        JsonNode card = json.readTree(response.body());
        System.out.println("agent card: HTTP " + response.statusCode() + ", " + card.get("name").asString() + ", "
                + card.at("/provider/organization").asString() + " " + card.get("version").asString()
                + ", protocol " + card.get("protocolVersion").asString() + ", url " + card.get("url").asString().replaceAll(":\\d+", ":PORT"));
        card.get("skills").forEach(skill -> System.out.println("  skill: " + skill.get("name").asString()
                + " - " + skill.get("description").asString()));
        System.out.println("  securitySchemes: " + card.get("securitySchemes"));
    }

    @Test
    void messageWithoutToken() throws Exception {
        var response = sendMessage("Where is my order NW-10442?", null);
        System.out.println("no token: HTTP " + response.statusCode());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void tokenWithoutA2aAuthority() throws Exception {
        var response = sendMessage("Where is my order NW-10442?", tokens.tokenFor("partner-agent", "orders:read"));
        System.out.println("orders:read only: HTTP " + response.statusCode());
        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void orderStatusOverA2a() throws Exception {
        var response = sendMessage("Where is my order NW-10442?", tokens.tokenFor("partner-agent", "a2a:invoke orders:read"));
        System.out.println("a2a:invoke orders:read: " + summary(response));
    }

    @Test
    void agentWithoutItsAuthority() throws Exception {
        var response = sendMessage("Where is my order NW-10442?", tokens.tokenFor("partner-agent", "a2a:invoke"));
        System.out.println("a2a:invoke only, order question: " + summary(response));
    }

    @Test
    void internalAgentNeedsItsOwnAuthority() throws Exception {
        var partner = sendMessage("Summarise yesterday's refunds", tokens.tokenFor("partner-agent", "a2a:invoke"));
        System.out.println("a2a:invoke only, refunds question: " + summary(partner));
        var finance = sendMessage("Summarise yesterday's refunds", tokens.tokenFor("finance-agent", "a2a:invoke finance:read"));
        System.out.println("a2a:invoke finance:read, refunds question: " + summary(finance));
        assertThat(summary(partner)).startsWith("error");
        assertThat(summary(finance)).contains("149.97 GBP");
    }
}
