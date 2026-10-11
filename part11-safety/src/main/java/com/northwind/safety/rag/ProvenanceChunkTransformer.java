package com.northwind.safety.rag;

import com.embabel.agent.rag.ingestion.AbstractChunkTransformer;
import com.embabel.agent.rag.ingestion.ChunkTransformationContext;
import com.embabel.agent.rag.model.Chunk;

import java.util.Map;

/**
 * Chapter 83: every chunk records where it came from and how far it is trusted. The label is derived
 * from the source location, decided by NorthWind, never from anything the document says about itself.
 */
public class ProvenanceChunkTransformer extends AbstractChunkTransformer {

    @Override
    public Map<String, Object> additionalMetadata(Chunk chunk, ChunkTransformationContext context) {
        String uri = context.getDocument() == null ? "" : context.getDocument().getUri();
        return Map.of("trust", uri.contains("/official/") ? "official" : "community", "source", uri);
    }
}
