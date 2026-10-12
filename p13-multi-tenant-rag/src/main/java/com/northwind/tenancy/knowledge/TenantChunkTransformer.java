package com.northwind.tenancy.knowledge;

import com.embabel.agent.rag.ingestion.AbstractChunkTransformer;
import com.embabel.agent.rag.ingestion.ChunkTransformationContext;
import com.embabel.agent.rag.model.Chunk;

import java.util.Map;

/**
 * Chapter 105: every chunk is labelled with the tenant that owns it and the audience it is for. The labels come
 * from where NorthWind stored the document (northwind://tenants/{tenant}/[staff/]...), never from its content.
 */
public class TenantChunkTransformer extends AbstractChunkTransformer {

    @Override
    public Map<String, Object> additionalMetadata(Chunk chunk, ChunkTransformationContext context) {
        String uri = context.getDocument() == null ? "" : context.getDocument().getUri();
        return Map.of("tenant", TenantKnowledge.tenantOf(uri),
                "audience", uri.contains("/staff/") ? "staff" : "customer",
                "source", uri);
    }
}
