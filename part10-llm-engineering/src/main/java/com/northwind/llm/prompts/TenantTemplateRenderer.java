package com.northwind.llm.prompts;

import com.embabel.common.textio.template.JinjavaTemplateRenderer;
import com.embabel.common.textio.template.NoSuchTemplateException;
import com.embabel.common.textio.template.TemplateRenderer;

import java.util.Map;

/**
 * Chapter 75: templates that live outside the jar, one set per tenant (here a map standing in for a
 * database table). Rendering is delegated to Embabel's Jinja renderer, so the syntax is the same.
 */
public class TenantTemplateRenderer implements TemplateRenderer {

    private final Map<String, String> templates;
    private final JinjavaTemplateRenderer jinja = new JinjavaTemplateRenderer();

    public TenantTemplateRenderer(Map<String, String> templates) {
        this.templates = Map.copyOf(templates);
    }

    @Override
    public String load(String name) {
        String template = templates.get(name);
        if (template == null) {
            throw new NoSuchTemplateException(name);
        }
        return template;
    }

    @Override
    public String renderLoadedTemplate(String name, Map<String, ?> model) {
        return jinja.renderLiteralTemplate(load(name), model);
    }

    @Override
    public String renderLiteralTemplate(String template, Map<String, ?> model) {
        return jinja.renderLiteralTemplate(template, model);
    }
}
