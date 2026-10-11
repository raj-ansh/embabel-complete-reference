package com.northwind.mcpclient;

import com.embabel.agent.core.ToolGroup;
import com.embabel.agent.tools.mcp.McpToolFactory;
import com.northwind.mcpclient.security.CurrentUserToken;
import com.northwind.mcpclient.service.ShopAssistant;
import com.northwind.partner.PartnerCatalogServer;
import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** P12: an Embabel application consuming tools from an MCP server over real HTTP. */
@SpringBootTest(classes = McpClientApplication.class, properties = "spring.ai.mcp.server.enabled=false")
@ActiveProfiles("lab")
class McpClientTest {

    static ConfigurableApplicationContext server;

    @DynamicPropertySource
    static void startServer(DynamicPropertyRegistry registry) {
        server = new SpringApplicationBuilder(PartnerCatalogServer.class)
                .properties("server.port=0", "spring.ai.mcp.server.name=partner-catalog", "spring.ai.mcp.server.protocol=STREAMABLE",
                        "spring.ai.mcp.client.enabled=false", "spring.main.banner-mode=off")
                .run();
        int port = server.getEnvironment().getProperty("local.server.port", Integer.class);
        registry.add("spring.ai.mcp.client.streamable-http.connections.northwind.url", () -> "http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        server.close();
    }

    @Autowired List<McpSyncClient> clients;
    @Autowired @Qualifier("northwindCatalog") ToolGroup catalog;
    @Autowired McpToolFactory mcpTools;
    @Autowired ShopAssistant assistant;

    @Test
    void whatTheServerOffersAndWhatTheGroupAccepts() {
        System.out.println("server offers:  " + clients.getFirst().listTools().tools().stream().map(t -> t.name()).toList());
        System.out.println("group accepts:  " + catalog.getTools().stream().map(t -> t.getDefinition().getName()).toList());
        assertThat(catalog.getTools()).extracting(t -> t.getDefinition().getName())
                .noneMatch(n -> n.contains("deleteAllOrders") || n.contains("partnerDeals"));
    }

    @Test
    void assistantUsesTheToolGroup() {
        System.out.println("answer: " + assistant.ask("Is the Ridgeline tent in stock?", "acme-outdoors", "req-1001"));
    }

    @Test
    void usersTokenIsRelayedToTheServer() {
        PartnerCatalogServer.LAST_AUTHORIZATION.set(null);
        CurrentUserToken.runAs("token-for-asha", () -> assistant.ask("Is the Ridgeline tent in stock?", "acme-outdoors", "req-1002"));
        System.out.println("Authorization header seen by the server (with a user): " + PartnerCatalogServer.LAST_AUTHORIZATION.get());
        assertThat(PartnerCatalogServer.LAST_AUTHORIZATION.get()).isEqualTo("Bearer token-for-asha");
        assistant.ask("Is the Ridgeline tent in stock?", "acme-outdoors", "req-1003");
        System.out.println("Authorization header seen by the server (no user):     " + PartnerCatalogServer.LAST_AUTHORIZATION.get());
        assertThat(PartnerCatalogServer.LAST_AUTHORIZATION.get()).isEqualTo("null");
    }

    @Test
    void factoryGivesIndividualTools() {
        System.out.println("tool by name: " + mcpTools.requiredToolByName("stockLevel").getDefinition().getName());
        System.out.println("answer: " + assistant.askWithOneTool("Is the Ridgeline tent in stock?"));
        System.out.println("missing tool: " + mcpTools.toolByName("refundEverything"));
    }

    @Test
    void factoryBuildsAProgressiveFacade() {
        var catalogue = mcpTools.unfoldingByName("northwindCatalogue",
                "NorthWind catalogue: stock levels and product details. Invoke to see the tools.",
                Set.of("stockLevel", "productDetails"));
        System.out.println("facade: " + catalogue.getDefinition().getName() + " - " + catalogue.getDefinition().getDescription());
        System.out.println("inner tools: " + catalogue.getInnerTools().stream().map(t -> t.getDefinition().getName()).sorted().toList());
    }
}
