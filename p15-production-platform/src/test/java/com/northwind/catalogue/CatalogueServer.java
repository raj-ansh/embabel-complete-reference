package com.northwind.catalogue;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * Test only: the catalogue team's MCP server, built with plain Spring AI. It offers the reviewed
 * productsInCategory tool and a deleteProduct tool that P15's allowlist must reject.
 */
@SpringBootApplication(excludeName = {
        "com.embabel.agent.autoconfigure.platform.AgentPlatformAutoConfiguration",
        "com.embabel.agent.autoconfigure.platform.cache.AgentCacheProviderAutoConfiguration",
        "com.embabel.agent.autoconfigure.platform.cache.CacheSnapshotStoreAutoConfiguration",
        "com.embabel.agent.autoconfigure.models.ollama.AgentOllamaAutoConfiguration",
        "com.embabel.agent.autoconfigure.observability.OpenTelemetrySdkAutoConfiguration",
        "com.embabel.agent.autoconfigure.observability.MicrometerTracingAutoConfiguration",
        "com.embabel.agent.autoconfigure.observability.ObservabilityAutoConfiguration",
        "com.embabel.agent.autoconfigure.observability.TrackedAspectAutoConfiguration"})
public class CatalogueServer {

    /** The stand-in is an internal test service: no authentication. */
    @Bean
    SecurityFilterChain openForTests(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(r -> r.anyRequest().permitAll()).csrf(c -> c.disable()).build();
    }

    @Component
    public static class CatalogueTools {

        @McpTool(name = "productsInCategory", description = "Products in a category (tent, stove or bag) with price and weight")
        public String productsInCategory(@McpToolParam(description = "tent, stove or bag") String category) {
            return switch (category) {
                case "tent" -> "TENT-2P Ridgeline 2-Person Tent, 2.10 kg, 199 GBP; "
                        + "TENT-UL1 Featherline 1-Person Tent, 0.95 kg, 279 GBP; "
                        + "TENT-4P Basecamp 4-Person Tent, 5.40 kg, 329 GBP";
                case "stove" -> "STOVE-1 Trailhead Stove, 0.22 kg, 49 GBP";
                default -> "No products in " + category;
            };
        }

        @McpTool(name = "deleteProduct", description = "Delete a product from the catalogue")
        public String deleteProduct(@McpToolParam(description = "SKU") String sku) {
            return "DELETED " + sku;
        }
    }
}
