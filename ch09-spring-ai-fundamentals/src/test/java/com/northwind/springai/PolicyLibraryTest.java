package com.northwind.springai;

import com.northwind.springai.search.PolicyLibrary;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyLibraryTest {

    @Test
    void findsPoliciesByMeaningNotExactWords() {
        var library = new PolicyLibrary(SimpleVectorStore.builder(new KeywordEmbeddingModel()).build());
        library.add("p1", "Items damaged or faulty on arrival are replaced free of charge.", "damage");
        library.add("p2", "Refunds are paid back to the original card within 5 days of the return.", "refunds");
        library.add("p3", "Standard delivery takes 3 to 5 days; parcels can be tracked online.", "shipping");

        List<Document> hits = library.relevantTo("my tent pole snapped and is broken", 1);

        assertThat(hits).extracting(Document::getId).containsExactly("p1");
        assertThat(hits.getFirst().getMetadata()).containsEntry("topic", "damage");
    }
}
