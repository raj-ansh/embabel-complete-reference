package com.northwind.mcpclient.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.tools.mcp.McpToolFactory;
import com.northwind.mcpclient.config.McpToolsConfig;
import org.springframework.stereotype.Service;

import java.util.Map;

/** P12: a shop assistant whose catalogue tools live in another application, reached over MCP. */
@Service
public class ShopAssistant {

    private final Ai ai;
    private final McpToolFactory mcpTools;

    public ShopAssistant(Ai ai, McpToolFactory mcpTools) {
        this.ai = ai;
        this.mcpTools = mcpTools;
    }

    /**
     * Uses the whole (filtered) tool group by role, as with any Embabel tool group (Chapter 40). The
     * correlation id comes from the incoming request (a header or the current trace), so logs on both
     * sides of the MCP call can be joined.
     */
    public String ask(String question, String tenant, String correlationId) {
        return ai.withLlmByRole("balanced")
                .withToolGroup(McpToolsConfig.CATALOG_ROLE)
                .withToolCallContext(Map.of(
                        "correlationId", correlationId,
                        "tenant", tenant,
                        "customerEmail", "asha.patel@example.com"))     // not on the allowlist: stays here
                .generateText(question);
    }

    /** Uses one named MCP tool, obtained from the factory. */
    public String askWithOneTool(String question) {
        return ai.withLlmByRole("balanced")
                .withTool(mcpTools.requiredToolByName("stockLevel"))
                .generateText(question);
    }
}
