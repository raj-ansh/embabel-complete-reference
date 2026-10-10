package com.northwind.docs;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.common.core.types.TextSimilaritySearchRequest;
import com.northwind.docs.agent.Docs.Asker;
import com.northwind.docs.agent.Docs.Audience;
import com.northwind.docs.agent.Docs.DocQuestion;
import com.northwind.docs.agent.Docs.GroundedAnswer;
import com.northwind.docs.rag.KnowledgeIngestor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** P7: ingestion, search and access control, with an offline embedder and a stubbed chat model. */
@TestPropertySource(properties = "northwind.rag.embedder=hashing")
class DocumentRagTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    LuceneSearchOperations store;

    @Autowired
    KnowledgeIngestor ingestor;

    @Test
    void theKnowledgeBaseIsIngestedOnceAtStartup() {
        System.out.println("store:  " + store.info());
        KnowledgeIngestor.IngestionReport again = ingestor.ingestAll();
        System.out.println("again:  " + again);

        assertThat(store.info().getDocumentCount()).isEqualTo(5);
        assertThat(again.ingested()).isEmpty();                     // same uris: nothing re-ingested
        assertThat(again.skipped()).contains("northwind://docs/staff/expenses-policy.pdf");
    }

    @Test
    void pdfContentIsSearchable() {
        var results = store.vectorSearch(TextSimilaritySearchRequest.create("hotel cost cap per night", 0.0, 2), Chunk.class);
        results.forEach(r -> System.out.printf("pdf hit %.2f %s%n", r.getScore(),
                r.getMatch().getMetadata().get("root_document_title")));
        assertThat(results.getFirst().getMatch().getUrtext()).contains("140 per night");
    }

    private String searchAs(Audience audience, String query) {
        AtomicReference<String> seen = new AtomicReference<>();
        whenCreateObject(p -> p.contains("<question>"), GroundedAnswer.class, interaction -> {
            List<String> names = interaction.getTools().stream().map(t -> t.getDefinition().getName()).toList();
            System.out.println(audience + " tools: " + names);
            Tool vector = interaction.getTools().stream()
                    .filter(t -> t.getDefinition().getName().equals("policies_vectorSearch")).findFirst().orElseThrow();
            seen.set(vector.call("{\"query\": \"" + query + "\", \"topK\": 3}").toString());   // what the model would get
            return true;
        }).thenReturn(new GroundedAnswer("…", List.of(), true));

        AgentInvocation.create(agentPlatform, GroundedAnswer.class)
                .invoke(new DocQuestion(query), new Asker("U-1", audience));
        System.out.println(audience + " search result, first lines:\n"
                + String.join("\n", seen.get().lines().limit(4).toList()));
        return seen.get();
    }

    @Test
    void customersCannotSearchStaffDocuments() {
        String customerView = searchAs(Audience.CUSTOMER, "who can approve refunds over 100");
        System.out.println("CUSTOMER sees staff guide: " + customerView.contains("Staff Guide"));
        assertThat(customerView).doesNotContain("Staff Guide", "support lead");
    }

    @Test
    void staffCanSearchEverything() {
        String staffView = searchAs(Audience.STAFF, "who can approve refunds over 100");
        System.out.println("STAFF sees staff guide: " + staffView.contains("Staff Guide"));
        assertThat(staffView).contains("Staff Guide: Refund Authority");
    }

    @Test
    void chunksCarryLogicalUrisNotFilePaths() {
        Chunk any = store.findAll().getFirst();
        System.out.println("chunk text starts:\n" + any.getText().lines().limit(3).reduce((a, b) -> a + "\n" + b).orElse(""));
        assertThat(any.getText()).contains("northwind://docs/").doesNotContain("file:");
    }
}
