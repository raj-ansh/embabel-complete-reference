package com.northwind.safety.rag;

import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.transform.ChainedChunkTransformer;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Chapter 83: a knowledge base that defends against poisoning at both ends. At ingestion, documents that
 * address an AI are quarantined and every chunk is labelled with its provenance. At retrieval, answers that
 * matter (safety, warranty) search official sources only.
 */
@Component
public class KnowledgeBase implements AutoCloseable {

    public record IngestionReport(List<String> ingested, List<String> quarantined) {
    }

    private static final Pattern ADDRESSES_AN_AI = Pattern.compile("(?i)\\b(AI|assistant|chatbot|language model)\\s*(assistant)?\\s*:");

    private final LuceneSearchOperations store = LuceneSearchOperations.withName("knowledge")
            .withEmbeddingService(new HashingEmbeddingService())
            .withChunkerConfig(new ContentChunker.Config(800, 100, 32))
            .withChunkTransformer(new ChainedChunkTransformer(List.of(new ProvenanceChunkTransformer())))
            .build();
    private final TikaHierarchicalContentReader reader = new TikaHierarchicalContentReader();

    public IngestionReport ingest(Path root) {
        List<String> ingested = new ArrayList<>();
        List<String> quarantined = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".md")).sorted().toList()) {
                String uri = "northwind://knowledge/" + root.relativize(file).toString().replace('\\', '/');
                if (ADDRESSES_AN_AI.matcher(Files.readString(file)).find()) {
                    quarantined.add(uri + " (contains text addressed to an AI)");   // for human review
                    continue;
                }
                store.writeAndChunkDocument(reader.parseFile(file.toAbsolutePath().toFile(), uri));
                ingested.add(uri);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new IngestionReport(ingested, quarantined);
    }

    /** Search tools for the model. Safety-critical questions use official sources only. */
    public ToolishRag search(boolean officialOnly) {
        var rag = new ToolishRag("knowledge", "NorthWind product knowledge", store);
        return officialOnly ? rag.withMetadataFilter(PropertyFilter.Companion.eq("trust", "official")) : rag;
    }

    @Override
    public void close() {
        store.close();
    }
}
