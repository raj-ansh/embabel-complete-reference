package com.northwind.mcpserver.config;

import com.embabel.agent.api.tool.ToolObject;
import com.embabel.agent.mcpserver.McpToolExport;
import com.northwind.mcpserver.tools.CatalogTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P11: besides exported goals (published automatically), plain tools can be published too. Each
 * McpToolExport bean adds its tools to the MCP server.
 */
@Configuration
public class McpExports {

    @Bean
    public McpToolExport catalogTools() {
        return McpToolExport.fromToolObject(new ToolObject(new CatalogTools()));
    }
}
