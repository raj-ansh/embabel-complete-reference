package com.northwind.content.domain;

import java.util.List;

/** P10's domain. Everything here may sit in a paused process for days, so it is all plain records. */
public final class Copy {

    private Copy() {
    }

    /** What the copy must say. */
    public record CopyBrief(String sku, String productName, List<String> keyFacts, int maxWords) {
    }

    /** A version of the product description. */
    public record Draft(String text, int round) {
    }

    /** The editor's decision on a draft, supplied by a person through WaitFor. */
    public record EditorFeedback(boolean approved, String comments, String editor) {
    }

    public enum Status { PUBLISHED, ESCALATED }

    /** How the workflow ended. */
    public record CopyOutcome(String sku, Status status, String text, int rounds, String decidedBy) {
    }
}
