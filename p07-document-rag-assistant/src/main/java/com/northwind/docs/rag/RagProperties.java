package com.northwind.docs.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** Every RAG knob in one place: where the knowledge is, where the index lives, how text is chunked. */
@ConfigurationProperties("northwind.rag")
public record RagProperties(Path knowledgeDir,
                            String indexPath,
                            String embedder,
                            int maxChunkSize,
                            int overlapSize,
                            boolean ingestOnStartup) {
}
