package com.northwind.docs;

import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.transform.AddTitlesChunkTransformer;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.service.ResultExpander;
import com.embabel.agent.rag.tools.ResultsEvent;
import com.embabel.agent.rag.tools.SearchDefaults;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.agent.rag.tools.TryHyDE;
import com.embabel.common.core.types.TextSimilaritySearchRequest;
import com.northwind.docs.rag.FaqStore;
import com.northwind.docs.rag.HashingEmbeddingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapters 55-60: embeddings, chunking, search operations, ToolishRag options and a custom store. */
class RagTechniquesTest {

    static final HashingEmbeddingService EMBEDDINGS = new HashingEmbeddingService();
    static LuceneSearchOperations store;

    static LuceneSearchOperations ingest(int maxChunkSize) throws Exception {
        var s = LuceneSearchOperations.withName("policies").withEmbeddingService(EMBEDDINGS)
                .withChunkerConfig(new ContentChunker.Config(maxChunkSize, Math.min(100, maxChunkSize / 4), 32))
                .withChunkTransformer(AddTitlesChunkTransformer.INSTANCE).build();
        var reader = new TikaHierarchicalContentReader();
        Path root = Path.of("knowledge").toAbsolutePath();
        try (var files = Files.walk(root)) {
            for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
                s.writeAndChunkDocument(reader.parseFile(f.toFile(),
                        "northwind://docs/" + root.relativize(f).toString().replace('\\', '/')));
            }
        }
        return s;
    }

    @BeforeAll
    static void setUp() throws Exception {
        store = ingest(800);
    }

    static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;                                  // both vectors are unit length
    }

    private static String first(String text, int n) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.length() <= n ? flat : flat.substring(0, n) + "…";
    }

    @Test
    void embeddingsCaptureSharedWordsNotMeaning() {
        String query = "How long do I have to return my boots?";
        String[] candidates = {
                "You can return most items within 30 days of delivery.",
                "Boots: return them within the window for a full refund.",
                "Can I send footwear back for my money?",
                "Tents over 10 kg ship by freight."};
        for (String c : candidates) {
            System.out.printf(Locale.ROOT, "cosine %.2f  %s%n", cosine(EMBEDDINGS.embed(query), EMBEDDINGS.embed(c)), c);
        }
        assertThat(cosine(EMBEDDINGS.embed(query), EMBEDDINGS.embed(candidates[2])))
                .isLessThan(cosine(EMBEDDINGS.embed(query), EMBEDDINGS.embed(candidates[1])));
    }

    @Test
    void chunkSizeChangesWhatTheModelGets() throws Exception {
        for (int size : new int[]{200, 400, 800, 1600}) {
            var s = ingest(size);
            System.out.printf("maxChunkSize %4d -> %2d chunks, average %.0f characters%n",
                    size, s.info().getChunkCount(), s.info().getAverageChunkLength());
            s.close();
        }
    }

    @Test
    void vectorAndTextSearchFindDifferentThings() {
        var vector = store.vectorSearch(TextSimilaritySearchRequest.create("money back for broken gear", 0.0, 2), Chunk.class);
        var text = store.textSearch(TextSimilaritySearchRequest.create("+refund -gift", 0.0, 2), Chunk.class);
        var phrase = store.textSearch(TextSimilaritySearchRequest.create("\"original payment method\"", 0.0, 2), Chunk.class);
        vector.forEach(r -> System.out.printf(Locale.ROOT, "vector %.2f %s%n", r.getScore(), first(r.getMatch().getUrtext(), 70)));
        text.forEach(r -> System.out.printf(Locale.ROOT, "text   %.2f %s%n", r.getScore(), first(r.getMatch().getUrtext(), 70)));
        phrase.forEach(r -> System.out.printf(Locale.ROOT, "phrase %.2f %s%n", r.getScore(), first(r.getMatch().getUrtext(), 70)));
        assertThat(phrase.getFirst().getMatch().getUrtext()).contains("original payment method");
    }

    @Test
    void expansionAddsContext() {
        Chunk hit = store.vectorSearch(TextSimilaritySearchRequest.create("hotel per night", 0.0, 1), Chunk.class)
                .getFirst().getMatch();
        var sequence = store.expandResult(hit.getId(), ResultExpander.Method.SEQUENCE, 1);
        var zoomOut = store.expandResult(hit.getId(), ResultExpander.Method.ZOOM_OUT, 1);
        System.out.println("hit:      " + first(hit.getUrtext(), 60));
        System.out.println("SEQUENCE: " + sequence.size() + " " + sequence.stream().map(e -> e.getClass().getSimpleName()).toList());
        System.out.println("ZOOM_OUT: " + zoomOut.size() + " " + zoomOut.stream().map(e -> e.getClass().getSimpleName()).toList());
    }

    @Test
    void toolishRagOptions() {
        List<ResultsEvent> events = new ArrayList<>();
        var rag = new ToolishRag("policies", "NorthWind policy documents", store)
                .withListener(events::add)
                .withSearchDefaults(new SearchDefaults(0.3, 0.0))
                .withHint(TryHyDE.withContext("NorthWind Outfitters customer policies"));

        Tool vector = rag.tools().stream().filter(t -> t.getDefinition().getName().equals("policies_vectorSearch")).findFirst().orElseThrow();
        vector.call("{\"query\": \"warranty for leaking boots\", \"topK\": 2}");
        assertThat(events).hasSize(1);
        System.out.println("contribution with HyDE hint:\n" + rag.contribution());

        ToolishRag eager = rag.withEagerSearchAbout("returning damaged items", 2);
        System.out.println("events after withEagerSearchAbout: " + events.size());
        String contribution = eager.contribution();
        System.out.println("events after contribution():     " + events.size());
        System.out.println("eager contribution mentions the damaged-items section: " + contribution.contains("Damaged or faulty items"));
        events.forEach(e -> System.out.println("listener: query='" + e.getQuery() + "' results=" + e.getResults().size()));
        assertThat(events).hasSize(2);
    }

    @Test
    void aFileBasedIndexSurvivesARestart(@org.junit.jupiter.api.io.TempDir Path indexDir) throws Exception {
        var first = LuceneSearchOperations.withName("policies").withEmbeddingService(EMBEDDINGS)
                .withChunkTransformer(AddTitlesChunkTransformer.INSTANCE)
                .withIndexPath(indexDir).build();
        var reader = new TikaHierarchicalContentReader();
        Path root = Path.of("knowledge").toAbsolutePath();
        try (var files = Files.walk(root)) {
            for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
                first.writeAndChunkDocument(reader.parseFile(f.toFile(),
                        "northwind://docs/" + root.relativize(f).toString().replace('\\', '/')));
            }
        }
        int before = first.info().getChunkCount();
        first.close();                                                   // "shut down"

        var second = LuceneSearchOperations.withName("policies").withEmbeddingService(EMBEDDINGS)
                .withChunkTransformer(AddTitlesChunkTransformer.INSTANCE)
                .withIndexPath(indexDir).buildAndLoadChunks();          // "start up": reuse the index on disk
        System.out.println("persistent index: before restart " + before + " chunks, after " + second.info().getChunkCount()
                + " (isPersistent=" + second.info().isPersistent() + ")");
        var hit = second.vectorSearch(TextSimilaritySearchRequest.create("hotel per night", 0.0, 1), Chunk.class);
        System.out.println("search after restart: " + first(hit.getFirst().getMatch().getUrtext(), 60));
        assertThat(second.info().getChunkCount()).isEqualTo(before);
        second.close();
    }

    @Test
    void aCustomStoreExposesOnlyWhatItSupports() {
        var faq = new FaqStore(List.of(
                new FaqStore.Faq("faq-1", "Do you ship to the islands?", "Yes, zone 2 deliveries take 4 working days."),
                new FaqStore.Faq("faq-2", "Can I change my order?", "Within one hour of ordering, from your account page.")));
        var rag = new ToolishRag("faq", "Frequently asked questions", faq);

        System.out.println("faq tools: " + rag.tools().stream().map(t -> t.getDefinition().getName()).toList());
        Tool text = rag.tools().getFirst();
        System.out.println("faq search: " + text.call("{\"query\": \"ship to the islands\", \"topK\": 1}"));
        assertThat(rag.tools()).extracting(t -> t.getDefinition().getName()).containsExactly("faq_textSearch");
    }
}
