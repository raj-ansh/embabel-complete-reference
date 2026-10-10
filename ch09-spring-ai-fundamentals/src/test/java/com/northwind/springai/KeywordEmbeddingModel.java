package com.northwind.springai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A toy embedding model for tests: each dimension counts words from one "concept".
 * Real embedding models learn thousands of dimensions; the idea — similar meaning, nearby vectors — is the same.
 */
public class KeywordEmbeddingModel implements EmbeddingModel {

    private static final List<List<String>> CONCEPTS = List.of(
            List.of("refund", "money", "return", "returns", "back"),
            List.of("ship", "shipping", "delivery", "parcel", "arrive", "arrived", "late"),
            List.of("broken", "damaged", "snapped", "faulty", "defect"),
            List.of("warranty", "guarantee", "years"));

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++) {
            embeddings.add(new Embedding(vector(request.getInstructions().get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vector(document.getText());
    }

    @Override
    public int dimensions() {
        return CONCEPTS.size();
    }

    private float[] vector(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split("\\W+");
        float[] v = new float[CONCEPTS.size()];
        for (String w : words) {
            for (int d = 0; d < CONCEPTS.size(); d++) {
                if (CONCEPTS.get(d).contains(w)) {
                    v[d] += 1f;
                }
            }
        }
        return v;
    }
}
