package com.northwind.docs.rag;

import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.transform.AddTitlesChunkTransformer;
import com.embabel.agent.rag.ingestion.transform.ChainedChunkTransformer;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.common.ai.model.DefaultModelSelectionCriteria;
import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.common.ai.model.ModelProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

/** Chapters 55, 58 and 59: the embedding model, the store and the document reader. */
@Configuration
public class RagConfiguration {

    @Bean
    EmbeddingService documentEmbeddings(RagProperties properties, ModelProvider models) {
        return "hashing".equals(properties.embedder())
                ? new HashingEmbeddingService()                                          // offline, deterministic
                : models.getEmbeddingService(DefaultModelSelectionCriteria.INSTANCE);   // embabel.models.default-embedding-model
    }

    @Bean(destroyMethod = "close")
    LuceneSearchOperations policyStore(EmbeddingService documentEmbeddings, RagProperties properties) {
        var builder = LuceneSearchOperations.withName("policies")
                .withEmbeddingService(documentEmbeddings)
                .withChunkerConfig(new ContentChunker.Config(properties.maxChunkSize(), properties.overlapSize(), 32))
                .withChunkTransformer(new ChainedChunkTransformer(List.of(
                        AddTitlesChunkTransformer.INSTANCE,          // context for retrieval and for the model
                        new AudienceChunkTransformer())));           // who may read each chunk
        if (properties.indexPath() != null && !properties.indexPath().isBlank()) {
            builder = builder.withIndexPath(Path.of(properties.indexPath()));
            return builder.buildAndLoadChunks();                     // reuse what is already on disk
        }
        return builder.build();
    }

    @Bean
    TikaHierarchicalContentReader documentReader() {
        return new TikaHierarchicalContentReader();                 // Markdown, HTML, PDF, Word, …
    }
}
