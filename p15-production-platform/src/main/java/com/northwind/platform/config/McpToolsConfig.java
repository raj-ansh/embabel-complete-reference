package com.northwind.platform.config;

import com.embabel.agent.core.ToolGroup;
import com.embabel.agent.core.ToolGroupDescription;
import com.embabel.agent.core.ToolGroupPermission;
import com.embabel.agent.tools.mcp.McpToolGroup;
import com.embabel.agent.tools.mcp.ToolCallContextMcpMetaConverter;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * P15: the product catalogue belongs to another team and is reached over MCP (Chapter 87). Only reviewed
 * tools are accepted, and only the tenant crosses the network as metadata (Chapter 88).
 */
@Configuration
public class McpToolsConfig {

    public static final String CATALOGUE_ROLE = "northwind-catalogue";

    static final Set<String> ALLOWED_TOOLS = Set.of("productsInCategory");

    @Bean
    public ToolGroup catalogue(List<McpSyncClient> mcpClients) {
        return new McpToolGroup(
                ToolGroupDescription.create("NorthWind product catalogue", CATALOGUE_ROLE),
                "catalogue-mcp", "NorthWind Catalogue",
                Set.of(ToolGroupPermission.INTERNET_ACCESS),
                mcpClients,
                callback -> ALLOWED_TOOLS.stream().anyMatch(callback.getToolDefinition().name()::endsWith),
                ToolCallContextMcpMetaConverter.allowKeys("tenant"));
    }
}
