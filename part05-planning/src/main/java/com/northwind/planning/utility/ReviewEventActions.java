package com.northwind.planning.utility;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chapter 33: reacting to an event with the Utility planner. There is no single goal;
 * every applicable action fires, highest net value (value - cost) first.
 */
@EmbabelComponent
public class ReviewEventActions {

    public record NewReview(String sku, int stars, String text) {
    }

    public record QualityAlert(String sku, String reason) {
    }

    public record ThankYouNote(String sku) {
    }

    public record FeatureCandidate(String sku, String quote) {
    }

    private final List<String> fired = new CopyOnWriteArrayList<>();

    @Action(description = "Alert the quality team about a poor review", value = 0.9, cost = 0.1,
            pre = {"spel:newReview.stars <= 2"})
    public QualityAlert alertQuality(NewReview review) {
        fired.add("alertQuality");
        return new QualityAlert(review.sku(), "Rated " + review.stars() + " stars");
    }

    @Action(description = "Consider featuring a glowing review on the product page", value = 0.6, cost = 0.1,
            pre = {"spel:newReview.stars == 5"})
    public FeatureCandidate considerFeaturing(NewReview review) {
        fired.add("considerFeaturing");
        return new FeatureCandidate(review.sku(), review.text());
    }

    @Action(description = "Send the reviewer a thank-you note", value = 0.3, cost = 0.1)
    public ThankYouNote thankReviewer(NewReview review) {
        fired.add("thankReviewer");
        return new ThankYouNote(review.sku());
    }

    public List<String> fired() {
        return List.copyOf(fired);
    }

    public void reset() {
        fired.clear();
    }
}
