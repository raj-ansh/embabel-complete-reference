package com.northwind.llm.prompts;

import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.prompt.persona.Persona;
import com.embabel.common.ai.prompt.PromptContributor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chapter 74: NorthWind's reusable prompt building blocks. Each one is written, reviewed and tested
 * once, then attached to any call that needs it.
 */
@Component
public class NorthwindPrompting {

    /** Fixed text: the same for every call. */
    public static final PromptContributor BRAND_VOICE = PromptContributor.fixed("""
            NorthWind voice: warm, plain British English, no exclamation marks, no jargon.
            Never promise refunds, delivery dates or compensation that a tool has not confirmed.""");

    /** A ready-made persona contributor. */
    public static final Persona NORA = new Persona(
            "Nora",
            "NorthWind Outfitters' customer care assistant",
            "Warm, concise and practical",
            "Resolve the customer's issue in a single reply");

    private final PromotionCalendar promotions;

    public NorthwindPrompting(PromotionCalendar promotions) {
        this.promotions = promotions;
    }

    /** Dynamic text: computed every time a prompt is built, so it is never stale. */
    public PromptContributor currentPromotion() {
        return PromptContributor.dynamic(() -> "Current promotion: " + promotions.current());
    }

    /** A reference: instructions plus the tools they refer to, attached together. */
    public LlmReference returnsPolicy() {
        return LlmReference.of("returns", "NorthWind returns policy", new ReturnsPolicyTools(),
                "Customers may return unused items within 30 days. "
                        + "Always call the eligibility tool before telling a customer whether an item can be returned.");
    }

    public List<PromptContributor> customerFacing() {
        return List.of(BRAND_VOICE, NORA, currentPromotion());
    }
}
