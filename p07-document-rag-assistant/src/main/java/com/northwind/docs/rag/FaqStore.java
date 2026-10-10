package com.northwind.docs.rag;

import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.model.Retrievable;
import com.embabel.agent.rag.service.TextSearch;
import com.embabel.common.core.types.SimilarityResult;
import com.embabel.common.core.types.SimpleSimilaritySearchResult;
import com.embabel.common.core.types.TextSimilaritySearchRequest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chapter 59: a custom RAG store. It implements ONLY TextSearch, because keyword matching over a short FAQ is
 * what it can do well. ToolishRag will therefore offer the model a text-search tool and nothing else.
 */
public class FaqStore implements TextSearch {

    public record Faq(String id, String question, String answer) {
    }

    private final List<Faq> faqs;

    public FaqStore(List<Faq> faqs) {
        this.faqs = List.copyOf(faqs);
    }

    @Override
    public boolean supportsType(String type) {
        return "Chunk".equals(type) || Chunk.class.getName().equals(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Retrievable> List<SimilarityResult<T>> textSearch(TextSimilaritySearchRequest request, Class<T> clazz) {
        Set<String> terms = words(request.getQuery());
        return faqs.stream()
                .map(faq -> {
                    Set<String> faqTerms = words(faq.question() + " " + faq.answer());
                    long hits = terms.stream().filter(faqTerms::contains).count();
                    double score = terms.isEmpty() ? 0 : (double) hits / terms.size();
                    // Chunk.create(text, parentId, metadata, id)
                    Chunk chunk = Chunk.create("Q: " + faq.question() + "\nA: " + faq.answer(), "faq",
                            Map.of("source", "faq"), faq.id());
                    return (SimilarityResult<T>) new SimpleSimilaritySearchResult<>((T) chunk, score);
                })
                .filter(r -> r.getScore() > request.getSimilarityThreshold())
                .sorted(Comparator.comparingDouble((SimilarityResult<T> r) -> r.getScore()).reversed())
                .limit(request.getTopK())
                .toList();
    }

    @Override
    public String getLuceneSyntaxNotes() {
        return "Plain words only; no operators. Results are ranked by the share of query words that match.";
    }

    private static Set<String> words(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(w -> w.length() > 2).collect(Collectors.toSet());
    }
}
