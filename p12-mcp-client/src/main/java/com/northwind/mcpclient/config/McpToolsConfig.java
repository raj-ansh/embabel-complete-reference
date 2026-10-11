package com.northwind.mcpclient.config;

import com.embabel.agent.core.ToolGroup;
import com.embabel.agent.core.ToolGroupDescription;
import com.embabel.agent.core.ToolGroupPermission;
import com.embabel.agent.spi.support.springai.SpringAiMcpToolFactory;
import com.embabel.agent.tools.mcp.McpToolFactory;
import com.embabel.agent.tools.mcp.McpToolGroup;
import com.embabel.agent.tools.mcp.ToolCallContextMcpMetaConverter;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * P12: NorthWind's MCP server consumed as an Embabel tool group. Only allowlisted tools become available,
 * and only allowlisted tool call context keys cross the network as MCP metadata.
 */
@Configuration
public class McpToolsConfig {

    public static final String CATALOG_ROLE = "northwind-catalog";

    /** The tools this application has reviewed and accepts from the server. Anything else is ignored. */
    static final Set<String> ALLOWED_TOOLS = Set.of("stockLevel", "productDetails", "orderStatus");

    @Bean
    public ToolGroup northwindCatalog(List<McpSyncClient> mcpClients) {
        return new McpToolGroup(
                ToolGroupDescription.create("NorthWind product catalogue and order status", CATALOG_ROLE),
                "northwind-mcp", "NorthWind",
                Set.of(ToolGroupPermission.INTERNET_ACCESS),
                mcpClients,
                callback -> ALLOWED_TOOLS.contains(baseName(callback.getToolDefinition().name())),
                ToolCallContextMcpMetaConverter.allowKeys("correlationId", "tenant"));
    }

    /** Individual MCP tools and progressive-disclosure facades, for code that wants them directly. */
    @Bean
    public McpToolFactory mcpToolFactory(List<McpSyncClient> mcpClients) {
        return new SpringAiMcpToolFactory(mcpClients);
    }

    /** Spring AI may prefix MCP tool names with the connection name; compare on the server's own name. */
    static String baseName(String toolName) {
        return ALLOWED_TOOLS.stream().filter(toolName::endsWith).findFirst().orElse(toolName);
    }
}
