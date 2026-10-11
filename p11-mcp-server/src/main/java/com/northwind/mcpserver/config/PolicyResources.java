package com.northwind.mcpserver.config;

import com.embabel.agent.mcpserver.sync.McpResourcePublisher;
import com.embabel.agent.mcpserver.sync.SyncResourceSpecificationFactory;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * P11: MCP resources are documents a client can read and give to its model as context. Unlike tools,
 * nothing runs: the client application decides when to read them.
 */
@Component
public class PolicyResources implements McpResourcePublisher {

    @Override
    public List<SyncResourceSpecification> resources() {
        return List.of(SyncResourceSpecificationFactory.staticSyncResourceSpecification(
                "northwind://policies/returns",
                "returns-policy",
                "NorthWind's returns policy, for answering customers' return questions",
                "Unused items can be returned within 30 days of delivery for a full refund. "
                        + "Tents that have been used can be returned within 14 days if faulty.",
                "text/plain"));
    }

    @Override
    public String infoString(Boolean verbose, int indent) {
        return "NorthWind policy resources";
    }
}
