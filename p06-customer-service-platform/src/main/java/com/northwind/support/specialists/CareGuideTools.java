package com.northwind.support.specialists;

import com.embabel.agent.api.annotation.LlmTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** The technical specialist's knowledge: NorthWind's care and repair guides (a stand-in for RAG, Part VIII). */
@Component
public class CareGuideTools {

    public record Guide(String title, String text) {
    }

    private static final List<Guide> GUIDES = List.of(
            new Guide("Re-proofing waterproof boots",
                    "Clean with lukewarm water, let dry naturally, then apply a wax or spray proofer. Never dry on a radiator."),
            new Guide("Tent seam care",
                    "If seams leak, dry the tent fully and apply seam sealer to the inside of the seam. Store dry and loose."),
            new Guide("Washing insulated jackets",
                    "Use a down-safe wash at 30°C, rinse twice, tumble dry low with dryer balls to restore loft."));

    @LlmTool(description = "Search NorthWind's care and repair guides; returns up to 2 matching guides")
    public List<Guide> searchCareGuides(@LlmTool.Param(description = "Topic, e.g. boots waterproofing") String topic) {
        List<String> words = List.of(topic.toLowerCase(Locale.ROOT).split("\\W+"));
        return GUIDES.stream()
                .filter(g -> words.stream().anyMatch(w -> w.length() > 3
                        && (g.title() + " " + g.text()).toLowerCase(Locale.ROOT).contains(w)))
                .limit(2).toList();
    }
}
