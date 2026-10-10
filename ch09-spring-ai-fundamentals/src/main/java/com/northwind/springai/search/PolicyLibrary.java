package com.northwind.springai.search;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/** Stores policy snippets as embeddings and finds the most relevant ones for a question. */
@Service
public class PolicyLibrary {

    private final VectorStore vectorStore;

    public PolicyLibrary(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void add(String id, String text, String topic) {
        vectorStore.add(List.of(Document.builder().id(id).text(text).metadata("topic", topic).build()));
    }

    public List<Document> relevantTo(String question, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK)
                .build());
    }
}
