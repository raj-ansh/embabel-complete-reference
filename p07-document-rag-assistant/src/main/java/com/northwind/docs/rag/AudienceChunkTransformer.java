package com.northwind.docs.rag;

import com.embabel.agent.rag.ingestion.AbstractChunkTransformer;
import com.embabel.agent.rag.ingestion.ChunkTransformationContext;
import com.embabel.agent.rag.model.Chunk;

import java.util.Map;

/**
 * Chapter 58: stamps every chunk with who may read it, derived from where the document came from.
 * Access control starts at ingestion: a chunk that isn't labelled can't be filtered later.
 */
public class AudienceChunkTransformer extends AbstractChunkTransformer {

    @Override
    public Map<String, Object> additionalMetadata(Chunk chunk, ChunkTransformationContext context) {
        String uri = context.getDocument() == null ? "" : context.getDocument().getUri();
        return Map.of("audience", uri.contains("/staff/") ? "staff" : "public");
    }
}
