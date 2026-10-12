package com.northwind.enterprise.knowledge;

import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** P14: NorthWind's policies and buying guides, searchable by every specialist (Part VIII). */
@Component
public class PolicyKnowledge implements AutoCloseable {

    private final LuceneSearchOperations store = LuceneSearchOperations.withName("policies")
            .withEmbeddingService(new HashingEmbeddingService())
            .withChunkerConfig(new ContentChunker.Config(800, 100, 32))
            .build();

    public int ingest() {
        var reader = new TikaHierarchicalContentReader();
        int count = 0;
        try {
            for (Resource doc : new PathMatchingResourcePatternResolver().getResources("classpath:policies/*.md")) {
                try (InputStream in = doc.getInputStream()) {
                    store.writeAndChunkDocument(reader.parseContent(in, "northwind://policies/" + doc.getFilename()));
                }
                count++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return count;
    }

    public ToolishRag search() {
        return new ToolishRag("policies", "NorthWind policies and buying guides", store);
    }

    @Override
    public void close() {
        store.close();
    }
}
