package com.northwind.platform.knowledge;

import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.ingestion.AbstractChunkTransformer;
import com.embabel.agent.rag.ingestion.ChunkTransformationContext;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.transform.ChainedChunkTransformer;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.tools.ToolishRag;
import com.northwind.platform.tenant.TenantContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * P15: each tenant's policies plus guides shared by all tenants, in one index. Chunks are labelled with
 * their owner ("shared" or a tenant) from the folder they were stored in; searches see the caller's
 * tenant and the shared guides only (Chapter 105).
 */
@Component
public class PolicyKnowledge implements AutoCloseable {

    private static class OwnerLabel extends AbstractChunkTransformer {
        @Override
        public Map<String, Object> additionalMetadata(Chunk chunk, ChunkTransformationContext context) {
            String uri = context.getDocument() == null ? "" : context.getDocument().getUri();
            String rest = uri.substring("northwind://policies/".length());
            return Map.of("owner", rest.substring(0, rest.indexOf('/')));
        }
    }

    private final LuceneSearchOperations store = LuceneSearchOperations.withName("policies")
            .withEmbeddingService(new HashingEmbeddingService())
            .withChunkerConfig(new ContentChunker.Config(800, 100, 32))
            .withChunkTransformer(new ChainedChunkTransformer(List.of(new OwnerLabel())))
            .build();

    public int ingest() {
        var reader = new TikaHierarchicalContentReader();
        int count = 0;
        try {
            for (Resource doc : new PathMatchingResourcePatternResolver().getResources("classpath:policies/*/*.md")) {
                String path = doc.getURL().toString().replace('\\', '/');
                String uri = "northwind://policies/" + path.substring(path.lastIndexOf("/policies/") + "/policies/".length());
                try (InputStream in = doc.getInputStream()) {
                    store.writeAndChunkDocument(reader.parseContent(in, uri));
                }
                count++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return count;
    }

    public ToolishRag searchFor(TenantContext tenant) {
        return new ToolishRag("policies", tenant.tenantName() + " policies and buying guides", store)
                .withMetadataFilter(PropertyFilter.Companion.in("owner", tenant.tenantId(), "shared"));
    }

    @Override
    public void close() {
        store.close();
    }
}
