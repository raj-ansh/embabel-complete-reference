package com.northwind.tenancy.knowledge;

import com.embabel.agent.rag.tools.ToolishRag;

/** Test access to the unfiltered search, which production code never exposes. */
public final class TenantKnowledgeAccess {

    private TenantKnowledgeAccess() {
    }

    public static ToolishRag unfiltered(TenantKnowledge knowledge) {
        return knowledge.unfiltered();
    }
}
