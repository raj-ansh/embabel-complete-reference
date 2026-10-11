package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.textio.template.TemplateRenderer;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Chapter 75: rendering classpath templates, and tenant templates through a custom renderer. */
@Service
public class TemplatedReplies {

    private final Ai ai;

    public TemplatedReplies(Ai ai) {
        this.ai = ai;
    }

    /** Renders classpath:/prompts/support/reply.jinja with the model map. */
    public String reply(Map<String, Object> model) {
        return ai.withLlmByRole("cheap")
                .rendering("support/reply")
                .generateText(model);
    }

    /** Same call, but the template comes from the tenant's own renderer. */
    public String reply(TemplateRenderer tenantTemplates, Map<String, Object> model) {
        return ai.withLlmByRole("cheap")
                .rendering("reply")
                .withTemplateRenderer(tenantTemplates)
                .generateText(model);
    }
}
