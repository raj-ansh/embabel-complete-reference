package com.northwind.docs.rag;

import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Chapter 58: ingestion. Each file gets a LOGICAL uri (northwind://docs/...), never a local path: the uri is
 * written into chunks the model reads, used for citations, and must be stable across machines.
 */
@Component
public class KnowledgeIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestor.class);
    private static final Set<String> SUPPORTED = Set.of(".md", ".pdf", ".html", ".docx", ".txt");

    public record IngestionReport(List<String> ingested, List<String> skipped) {
    }

    private final LuceneSearchOperations store;
    private final TikaHierarchicalContentReader reader;
    private final RagProperties properties;

    public KnowledgeIngestor(LuceneSearchOperations store, TikaHierarchicalContentReader reader, RagProperties properties) {
        this.store = store;
        this.reader = reader;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.ingestOnStartup()) {
            IngestionReport report = ingestAll();
            log.info("Knowledge base: {} ingested, {} already present; {}", report.ingested().size(),
                    report.skipped().size(), store.info());
        }
    }

    /** Ingests new documents; documents already in the store (same uri) are skipped. */
    public IngestionReport ingestAll() {
        List<String> ingested = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Path file : documents()) {
            String uri = logicalUri(file);
            if (store.existsRootWithUri(uri)) {
                skipped.add(uri);
                continue;
            }
            store.writeAndChunkDocument(reader.parseFile(file.toFile(), uri));
            ingested.add(uri);
        }
        return new IngestionReport(ingested, skipped);
    }

    /** Re-reads one document after it changed: delete everything derived from it, then ingest again. */
    public void reingest(Path file) {
        String uri = logicalUri(file);
        store.deleteRootAndDescendants(uri);
        store.writeAndChunkDocument(reader.parseFile(file.toFile(), uri));
    }

    public String logicalUri(Path file) {
        String relative = root().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
        return "northwind://docs/" + relative;
    }

    private Path root() {
        return properties.knowledgeDir().toAbsolutePath().normalize();
    }

    private List<Path> documents() {
        try (Stream<Path> files = Files.walk(root())) {
            return files.filter(Files::isRegularFile)
                    .filter(f -> SUPPORTED.stream().anyMatch(ext -> f.toString().toLowerCase().endsWith(ext)))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
