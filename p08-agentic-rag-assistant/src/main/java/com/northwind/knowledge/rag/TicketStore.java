package com.northwind.knowledge.rag;

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
 * Resolved support tickets: what actually fixed real customers' problems. A second knowledge source with a
 * different shape (short Q&A pairs, keyword search only) from the product manuals.
 */
public class TicketStore implements TextSearch {

    public record Ticket(String id, String question, String answer) {
    }

    private final List<Ticket> tickets;

    public TicketStore(List<Ticket> tickets) {
        this.tickets = List.copyOf(tickets);
    }

    @Override
    public boolean supportsType(String type) {
        return "Chunk".equals(type) || Chunk.class.getName().equals(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Retrievable> List<SimilarityResult<T>> textSearch(TextSimilaritySearchRequest request, Class<T> clazz) {
        Set<String> terms = words(request.getQuery());
        return tickets.stream()
                .map(t -> {
                    long hits = words(t.question() + " " + t.answer()).stream().filter(terms::contains).count();
                    double score = terms.isEmpty() ? 0 : Math.min(1.0, (double) hits / terms.size());
                    Chunk chunk = Chunk.create("Ticket " + t.id() + "\nProblem: " + t.question() + "\nResolution: " + t.answer(),
                            "tickets", Map.of("ticket", t.id()), t.id());
                    return (SimilarityResult<T>) new SimpleSimilaritySearchResult<>((T) chunk, score);
                })
                .filter(r -> r.getScore() > request.getSimilarityThreshold())
                .sorted(Comparator.comparingDouble((SimilarityResult<T> r) -> r.getScore()).reversed())
                .limit(request.getTopK())
                .toList();
    }

    @Override
    public String getLuceneSyntaxNotes() {
        return "Plain keywords only, e.g. 'stove cold flame'. Results are ranked by how many keywords match.";
    }

    private static Set<String> words(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(w -> w.length() > 3).collect(Collectors.toSet());
    }
}
