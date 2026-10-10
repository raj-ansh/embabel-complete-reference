package com.northwind.content.copy;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.stereotype.Service;

import java.util.List;

/** Turns a validated brief into typed, validated product copy. */
@Service
public class ProductCopyService {

    static final String COPY_RULES = """
            You write product copy for NorthWind Outfitters' online store.
            Use only the features given in the brief; never invent specifications, prices or awards.
            Write in US English. No exclamation marks in the headline.""";

    private final Ai ai;

    public ProductCopyService(Ai ai) {
        this.ai = ai;
    }

    public ProductCopy write(ProductBrief brief) {
        return ai.withLlm(LlmOptions.withLlmForRole("balanced").withTemperature(0.6).withMaxTokens(800))
                .withSystemPrompt(COPY_RULES)
                .creating(ProductCopy.class)
                .withExample("Copy for a trekking pole", EXAMPLE)
                .fromPrompt("""
                        Write product copy for this brief.

                        Product: %s (SKU %s), category %s
                        Audience: %s
                        Features:
                        %s""".formatted(brief.name(), brief.sku(), brief.category(), brief.audience(),
                        String.join("\n", brief.features().stream().map(f -> "- " + f).toList())));
    }

    static final ProductCopy EXAMPLE = new ProductCopy(
            "Summit Carbon Trekking Poles",
            "Lightweight carbon poles that lock firmly in seconds and pack down small, "
                    + "so you can save your knees on the descent and stow them when the trail flattens out.",
            List.of("Carbon shafts weigh 210 g per pole", "Flick-lock adjusts from 100 to 135 cm",
                    "Cork grips stay comfortable in heat"),
            List.of("trekking poles", "carbon hiking poles", "collapsible poles"),
            ProductCopy.Tone.PRACTICAL);
}
