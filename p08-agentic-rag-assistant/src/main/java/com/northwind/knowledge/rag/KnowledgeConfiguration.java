package com.northwind.knowledge.rag;

import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.transform.AddTitlesChunkTransformer;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.agent.rag.tools.TryHyDE;
import com.embabel.common.ai.model.DefaultModelSelectionCriteria;
import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.common.ai.model.ModelProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** P8: two knowledge sources, each exposed to the model as its own reference. */
@Configuration
public class KnowledgeConfiguration {

    @Bean
    EmbeddingService manualEmbeddings(@Value("${northwind.rag.embedder}") String embedder, ModelProvider models) {
        return "hashing".equals(embedder)
                ? new HashingEmbeddingService()
                : models.getEmbeddingService(DefaultModelSelectionCriteria.INSTANCE);
    }

    /** Product manuals: long, structured documents. Vector and text search, plus chunk expansion. */
    @Bean(destroyMethod = "close")
    LuceneSearchOperations manualStore(EmbeddingService manualEmbeddings,
                                       @Value("${northwind.rag.manuals-dir}") Path manualsDir) throws IOException {
        var store = LuceneSearchOperations.withName("manuals")
                .withEmbeddingService(manualEmbeddings)
                .withChunkerConfig(new ContentChunker.Config(700, 100, 32))
                .withChunkTransformer(AddTitlesChunkTransformer.INSTANCE)
                .build();
        var reader = new TikaHierarchicalContentReader();
        Path root = manualsDir.toAbsolutePath().normalize();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".md")).sorted().toList()) {
                store.writeAndChunkDocument(reader.parseFile(file.toFile(),
                        "northwind://manuals/" + root.relativize(file).toString().replace('\\', '/')));
            }
        }
        return store;
    }

    /** Resolved tickets: short Q&A pairs. Keyword search only. */
    @Bean
    TicketStore ticketStore() {
        try (InputStream in = new ClassPathResource("resolved-tickets.json").getInputStream()) {
            return new TicketStore(JsonMapper.builder().build().readValue(in, new TypeReference<List<TicketStore.Ticket>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Bean
    ToolishRag manuals(LuceneSearchOperations manualStore) {
        return new ToolishRag("manuals", "NorthWind product manuals: setup, use, safety, errors and repairs", manualStore)
                .withHint(TryHyDE.withContext("NorthWind outdoor gear manuals: tents, stoves, headlamps"));
    }

    @Bean
    ToolishRag tickets(TicketStore ticketStore) {
        return new ToolishRag("tickets", "Resolved customer support tickets: what fixed real customers' problems",
                ticketStore);
    }
}
