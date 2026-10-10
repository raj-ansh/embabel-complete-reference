package com.northwind.content.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.State;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.content.domain.Copy.CopyBrief;
import com.northwind.content.domain.Copy.CopyOutcome;
import com.northwind.content.domain.Copy.Draft;
import com.northwind.content.domain.Copy.EditorFeedback;
import com.northwind.content.domain.Copy.Status;

/**
 * P10: write → review → revise → approve, as explicit states.
 * A model writes; an editor (a person) reviews; the loop ends in publication or escalation.
 */
@Agent(description = "Write a product description, have an editor review it, revise until approved, then publish")
public class ProductCopyAgent {

    public static final int MAX_ROUNDS = 3;

    /** Every state implements Stage; only the interface carries @State (it is inherited). */
    @State
    public interface Stage {
    }

    static Draft write(CopyBrief brief, String feedback, String previous, int round, Ai ai) {
        String text = ai.withLlm(LlmOptions.withLlmForRole("writer").withTemperature(0.6))
                .generateText("""
                        Write a product description for NorthWind Outfitters in at most %d words.
                        Product: %s
                        Facts (use all, invent none): %s
                        %s""".formatted(brief.maxWords(), brief.productName(), String.join("; ", brief.keyFacts()),
                        previous == null ? "" : """
                                <previous-draft>
                                %s
                                </previous-draft>
                                <editor-feedback>
                                %s
                                </editor-feedback>
                                Revise the previous draft to address the feedback.""".formatted(previous, feedback)));
        return new Draft(text.strip(), round);
    }

    /** Entry: write the first draft and enter the review state. */
    @Action(description = "Write the first draft")
    public AwaitingReview firstDraft(CopyBrief brief, Ai ai) {
        return new AwaitingReview(brief, write(brief, null, null, 1, ai));
    }

    /** The draft is with the editor. The process pauses here, possibly for days. */
    public record AwaitingReview(CopyBrief brief, Draft draft) implements Stage {

        @Action(description = "Ask the editor to review the draft")
        public EditorFeedback requestReview() {
            return WaitFor.formSubmission("""
                    Review round %d of the description for %s:

                    %s""".formatted(draft.round(), brief.productName(), draft.text()), EditorFeedback.class);
        }

        /** Looping action: it may lead back to a state type seen before, so it clears the blackboard. */
        @Action(clearBlackboard = true, description = "Act on the editor's decision")
        public Stage decide(EditorFeedback feedback) {
            if (feedback.approved()) {
                return new Approved(brief, draft, feedback.editor());
            }
            if (draft.round() >= MAX_ROUNDS) {
                return new Escalated(brief, draft, feedback);
            }
            return new Revising(brief, draft, feedback);
        }
    }

    /** The editor asked for changes. */
    public record Revising(CopyBrief brief, Draft draft, EditorFeedback feedback) implements Stage {

        @Action(clearBlackboard = true, description = "Revise the draft using the editor's feedback")
        public AwaitingReview revise(Ai ai) {
            return new AwaitingReview(brief, write(brief, feedback.comments(), draft.text(), draft.round() + 1, ai));
        }
    }

    /** Terminal: the editor approved. */
    public record Approved(CopyBrief brief, Draft draft, String editor) implements Stage {

        @AchievesGoal(description = "The product description has been approved and published")
        @Action(description = "Publish the approved description")
        public CopyOutcome publish() {
            return new CopyOutcome(brief.sku(), Status.PUBLISHED, draft.text(), draft.round(), editor);
        }
    }

    /** Terminal: still not approved after MAX_ROUNDS; a person takes over. */
    public record Escalated(CopyBrief brief, Draft draft, EditorFeedback lastFeedback) implements Stage {

        @AchievesGoal(description = "The product description was escalated after too many revisions")
        @Action(description = "Hand the description to the content lead")
        public CopyOutcome escalate() {
            return new CopyOutcome(brief.sku(), Status.ESCALATED, draft.text(), draft.round(), lastFeedback.editor());
        }
    }
}
