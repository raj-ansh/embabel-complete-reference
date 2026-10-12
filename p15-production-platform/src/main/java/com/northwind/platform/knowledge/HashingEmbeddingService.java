package com.northwind.platform.knowledge;

import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.common.ai.model.ModelType;
import com.embabel.common.ai.model.PricingModel;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Chapter 55 (copied from P7 for P15): a tiny, deterministic embedding "model" using feature hashing. Each word (and each pair of
 * adjacent words) is hashed into one of DIMENSIONS buckets; the vector is L2-normalised so cosine similarity
 * is a dot product. It captures shared vocabulary, NOT meaning: "refund" and "money back" are unrelated to it.
 * Use it for offline development and repeatable tests; use a real embedding model in production.
 */
public class HashingEmbeddingService implements EmbeddingService {

    public static final int DIMENSIONS = 512;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "is", "are", "be", "it", "my", "i",
            "do", "does", "can", "you", "your", "we", "our", "with", "if", "how", "what", "when", "within");

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        String previous = null;
        for (String word : words) {
            if (word.isBlank() || STOP_WORDS.contains(word)) {
                continue;
            }
            String stem = word.endsWith("s") && word.length() > 3 ? word.substring(0, word.length() - 1) : word;
            vector[Math.floorMod(stem.hashCode(), DIMENSIONS)] += 1.0f;
            if (previous != null) {
                vector[Math.floorMod((previous + " " + stem).hashCode(), DIMENSIONS)] += 0.5f;
            }
            previous = stem;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm > 0) {
            float scale = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= scale;
            }
        }
        return vector;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public String getName() {
        return "hashing-" + DIMENSIONS;
    }

    @Override
    public String getProvider() {
        return "northwind";
    }

    @Override
    public ModelType getType() {
        return ModelType.EMBEDDING;
    }

    @Override
    public PricingModel getPricingModel() {
        return PricingModel.getALL_YOU_CAN_EAT();
    }
}
