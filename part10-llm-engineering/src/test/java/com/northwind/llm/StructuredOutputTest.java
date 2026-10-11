package com.northwind.llm;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.NativeStructuredOutputMode;
import com.northwind.llm.domain.Support.ProductSummary;
import com.northwind.llm.lab.ScriptedChatModel.LabReply;
import com.northwind.llm.service.ProductSummaries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 76: how Embabel asks for typed output, and what happens when the model gets it wrong. */
class StructuredOutputTest extends LabTest {

    static final String TENT = "Ridgeline 2-Person Tent, SKU TENT-2P. Weighs 2.1 kg, 3,000 mm flysheet, two doors.";

    @Autowired ProductSummaries summaries;
    @Autowired Ai ai;

    @Test
    void promptBasedStructuredOutput() {
        ProductSummary summary = summaries.summarise(TENT, NativeStructuredOutputMode.DISABLED);
        var prompt = standard().received().getLast();
        String system = prompt.getSystemMessage().getText();
        System.out.println("---- what Embabel added to the prompt ----");
        System.out.println(system.substring(system.indexOf("Your response should be in JSON format")));
        System.out.println("---- result ----");
        System.out.println(summary);
        assertThat(summary.highlights()).contains("2.1 kg");
    }

    @Test
    void nativeModeOnAModelWithoutNativeSupport() {
        ProductSummary summary = summaries.summarise(TENT, NativeStructuredOutputMode.ENABLED);
        String system = standard().received().getLast().getSystemMessage().getText();
        System.out.println("ENABLED on lab-standard -> schema still in prompt: " + system.contains("JSON Schema")
                + ", result: " + summary.sku());
    }

    @Test
    void invalidJsonIsRetried() {
        var attempts = new AtomicInteger();
        standard().rule("flaky", r -> r.mentions("Summarise the item"), r -> attempts.incrementAndGet() == 1
                ? LabReply.text("Sure! The Ridgeline is a great little tent that weighs 2.1 kg.")
                : LabReply.text("{\"sku\":\"TENT-2P\",\"name\":\"Ridgeline 2-Person Tent\",\"oneLiner\":\"Light\",\"highlights\":[]}"));

        ProductSummary summary = ai.withLlmByRole("balanced").createObject("Summarise the item TENT-2P.", ProductSummary.class);
        System.out.println("model calls: " + attempts.get() + ", result: " + summary);
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void examplesShowTheModelAGoodAnswer() {
        var example = new ProductSummary("STOVE-1", "Trailhead Stove", "Boils a litre in four minutes",
                List.of("piezo ignition", "220 g"));
        ai.withLlmByRole("balanced").creating(ProductSummary.class)
                .withExample("A good summary", example)
                .fromPrompt("Summarise the product described between the <product> tags.\n<product>" + TENT + "</product>");
        String all = standard().received().getLast().getContents();
        System.out.println(all.substring(all.indexOf("A good summary") - 40, all.indexOf("A good summary") + 220));
    }
}
