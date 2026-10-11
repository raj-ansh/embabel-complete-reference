package com.northwind.mcpclient;

import com.northwind.mcpclient.service.ShopAssistant;
import com.northwind.partner.PartnerCatalogServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Chapter 88: just-in-time initialisation. The application starts while the MCP server is unreachable;
 * the MCP handshake happens on first use, when a user (and their token) is present.
 */
@SpringBootTest(classes = McpClientApplication.class, properties = {
        "spring.ai.mcp.server.enabled=false",
        "spring.ai.mcp.client.initialized=false",
        "spring.ai.mcp.client.toolcallback.enabled=false",
        "embabel.agent.platform.tools.lazy-init=true"})
@ActiveProfiles("lab")
class LazyMcpClientTest {

    static int port;
    static ConfigurableApplicationContext server;

    @DynamicPropertySource
    static void serverAddress(DynamicPropertyRegistry registry) throws IOException {
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();                      // reserved address; nothing listening yet
        }
        registry.add("spring.ai.mcp.client.streamable-http.connections.northwind.url", () -> "http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Autowired ShopAssistant assistant;

    @Test
    void startsWithoutTheServerAndConnectsOnFirstUse() {
        System.out.println("application started; MCP server running: " + (server != null));
        server = new SpringApplicationBuilder(PartnerCatalogServer.class)
                .properties("server.port=" + port, "spring.ai.mcp.server.name=partner-catalog",
                        "spring.ai.mcp.server.protocol=STREAMABLE", "spring.ai.mcp.client.enabled=false",
                        "spring.main.banner-mode=off")
                .run();
        System.out.println("MCP server started; first use: " + assistant.ask("Is the Ridgeline tent in stock?", "acme-outdoors", "req-1001"));
    }
}
