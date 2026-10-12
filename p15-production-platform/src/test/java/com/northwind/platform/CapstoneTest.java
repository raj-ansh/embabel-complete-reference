package com.northwind.platform;

import com.embabel.agent.core.ToolGroup;
import com.northwind.catalogue.CatalogueServer;
import com.northwind.platform.security.LabTokens;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 112: the capstone end to end, over HTTP with JWTs, against stand-in billing (HTTP) and catalogue
 * (MCP) services, with the lab models.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lab")
class CapstoneTest {

    static ConfigurableApplicationContext catalogue;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) throws IOException {
        String billing = BillingStub.start();
        // command-line arguments, because they outrank this project's application.yml (properties() would not)
        catalogue = new SpringApplicationBuilder(CatalogueServer.class)
                .run("--server.port=0", "--spring.ai.mcp.server.enabled=true", "--spring.ai.mcp.server.name=catalogue",
                        "--spring.ai.mcp.server.protocol=STREAMABLE", "--spring.ai.mcp.client.enabled=false",
                        "--spring.main.banner-mode=off");
        int port = catalogue.getEnvironment().getProperty("local.server.port", Integer.class);
        GlobalOpenTelemetry.resetForTest();                         // Chapter 91
        registry.add("northwind.billing.url", () -> billing);
        registry.add("spring.ai.mcp.client.streamable-http.connections.catalogue.url", () -> "http://localhost:" + port);
    }

    @AfterAll
    static void stop() {
        catalogue.close();
    }

    @Autowired MockMvcTester mvc;
    @Autowired LabTokens tokens;
    @Autowired @Qualifier("catalogue") ToolGroup catalogueTools;

    private String acme() {
        return tokens.tokenFor("C-1001", "acme-outdoors", "support");
    }

    private String peak() {
        return tokens.tokenFor("C-2001", "peak-gear", "support");
    }

    private String acmeStaff() {
        return tokens.tokenFor("S-77", "acme-outdoors", "refunds:approve");
    }

    private String peakStaff() {
        return tokens.tokenFor("S-88", "peak-gear", "refunds:approve");
    }

    private String ask(String token, String who, String text) throws Exception {
        var result = mvc.post().uri("/api/requests").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"" + text + "\"}").exchange();
        String body = result.getResponse().getContentAsString().replaceAll("chunkId: \\S+ [0-9.]+ - ", "");
        System.out.println(who + ": \"" + text + "\" -> " + result.getResponse().getStatus() + " "
                + (body.length() > 160 ? body.substring(0, 160) + "..." : body));
        return body;
    }

    private MvcTestResult staff(String token, String method, String uri) {
        var request = method.equals("GET") ? mvc.get().uri(uri) : mvc.post().uri(uri);
        return request.header("Authorization", "Bearer " + token).exchange();
    }

    @Test
    void customersOfEachTenant() throws Exception {
        assertThat(ask(acme(), "Acme C-1001", "Where is my order NW-40001?")).contains("SHIPPED");
        assertThat(ask(peak(), "Peak C-2001", "Where is my order NW-40001?")).contains("No order");
        assertThat(ask(acme(), "Acme C-1001", "How long do I have to return a tent?")).contains("30 days");
        assertThat(ask(peak(), "Peak C-2001", "How long do I have to return a tent?")).contains("14 days");
        assertThat(ask(acme(), "Acme C-1001", "My card 4111 1111 1111 1111 was charged twice"))
                .contains("don't send card numbers");
    }

    @Test
    void researchUsesTheCatalogueOverMcp() throws Exception {
        System.out.println("catalogue tools accepted from MCP: " + catalogueTools.getTools().stream()
                .map(t -> t.getDefinition().getName()).toList());
        assertThat(ask(acme(), "Acme C-1001", "Which tent is lightest for a solo trek?")).contains("Featherline");
    }

    @Test
    void refundsAndStaffApprovalPerTenant() throws Exception {
        assertThat(ask(acme(), "Acme C-1001", "Please refund 20 pounds on invoice INV-7001")).contains("Refunded 20");
        assertThat(ask(acme(), "Acme C-1001", "Please refund 150 pounds on invoice INV-7002")).contains("for approval");

        System.out.println("customer token on the staff queue: " + staff(acme(), "GET", "/api/staff/refunds").getResponse().getStatus());
        System.out.println("Peak Gear staff queue: " + staff(peakStaff(), "GET", "/api/staff/refunds").getResponse().getContentAsString());
        String acmeQueue = staff(acmeStaff(), "GET", "/api/staff/refunds").getResponse().getContentAsString();
        System.out.println("Acme staff queue: " + acmeQueue);
        String id = acmeQueue.replaceAll("(?s).*\"id\":(\\d+).*", "$1");
        System.out.println("Peak Gear staff approving Acme's refund " + id + ": "
                + staff(peakStaff(), "POST", "/api/staff/refunds/" + id + "/approve").getResponse().getStatus());
        var approved = staff(acmeStaff(), "POST", "/api/staff/refunds/" + id + "/approve");
        System.out.println("Acme staff approving it: " + approved.getResponse().getStatus() + " "
                + approved.getResponse().getContentAsString());
        assertThat(approved).hasStatus(200);
    }

    @Test
    void noTokenNoService() {
        var result = mvc.post().uri("/api/requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hello\"}").exchange();
        System.out.println("no token: " + result.getResponse().getStatus());
        assertThat(result).hasStatus(401);
    }
}
