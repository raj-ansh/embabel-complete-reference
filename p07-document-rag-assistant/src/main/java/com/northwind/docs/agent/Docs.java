package com.northwind.docs.agent;

import java.util.List;

/** P7's domain. */
public final class Docs {

    private Docs() {
    }

    public enum Audience { CUSTOMER, STAFF }

    /** Who is asking, from authentication. Decides which documents may be searched. */
    public record Asker(String userId, Audience audience) {
    }

    public record DocQuestion(String question) {
    }

    /**
     * The answer, the documents it is based on, and an honest flag: false means the documents did not
     * answer the question, and the text says so rather than guessing.
     */
    public record GroundedAnswer(String answer, List<String> sources, boolean answeredFromDocuments) {
    }
}
