package com.northwind.llm;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.llm.domain.Messages.CustomerMessage;
import com.northwind.llm.domain.Messages.ShortReply;
import com.northwind.llm.prompts.TenantTemplateRenderer;
import com.northwind.llm.service.TemplatedReplies;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 75: what templates render to, and what they refuse to evaluate. */
class TemplatesTest extends LabTest {

    @Autowired TemplatedReplies replies;
    @Autowired AgentPlatform platform;

    @Test
    void classpathTemplate() {
        replies.reply(Map.of("customerName", "Asha", "tier", "Gold", "openOrders", List.of("NW-10442", "NW-10501"),
                "message", "Where is my tent? {{ 7 * 7 }}"));
        String rendered = mini().received().getLast().getUserMessage().getText();
        System.out.println(rendered);
        assertThat(rendered).contains("{{ 7 * 7 }}").doesNotContain("49");
    }

    @Test
    void systemPromptFromTemplateInsideAnAction() {
        AgentInvocation.create(platform, ShortReply.class)
                .invoke(new CustomerMessage("Asha", "Gold", List.of(), "Can I send the stove back?"));
        Prompt prompt = mini().received().getLast();
        System.out.println("---- system ----");
        System.out.println(prompt.getSystemMessage().getText());
        System.out.println("---- user ----");
        System.out.println(prompt.getUserMessage().getText());
        assertThat(prompt.getSystemMessage().getText()).contains("Gold members get free returns");
    }

    record Customer(String name, String tier) {
    }

    @Test
    void recordsVersusMaps() {
        var jinja = new com.embabel.common.textio.template.JinjavaTemplateRenderer();
        String template = "Customer: {{ c.name }} ({{ c.tier }})";
        System.out.println("record: " + jinja.renderLiteralTemplate(template, Map.of("c", new Customer("Asha", "Gold"))));
        System.out.println("map:    " + jinja.renderLiteralTemplate(template, Map.of("c", Map.of("name", "Asha", "tier", "Gold"))));
    }

    @Test
    void tenantTemplate() {
        var tenantTemplates = new TenantTemplateRenderer(Map.of("reply", """
                Write a short reply in the voice of Peak & Pine Outdoors, a small Highland shop.
                Sign off as "the Peak & Pine team".
                <message>{{ message }}</message>"""));
        replies.reply(tenantTemplates, Map.of("message", "Do you deliver to Skye?"));
        System.out.println(mini().received().getLast().getUserMessage().getText());
    }
}
