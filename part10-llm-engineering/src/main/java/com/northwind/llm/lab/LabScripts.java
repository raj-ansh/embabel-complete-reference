package com.northwind.llm.lab;

import com.northwind.llm.lab.ScriptedChatModel.LabReply;
import com.northwind.llm.lab.ScriptedChatModel.LabRequest;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rules behind the lab models. Each rule recognises one of Part X's prompts and answers the way
 * a well-behaved model would. Nothing here is "intelligent"; it exists so the framework can be
 * exercised end to end without a network connection or an API key.
 */
public final class LabScripts {

    private LabScripts() {
    }

    /** Small and cheap: good at classification, weak after tool calls (Chapter 70). */
    public static ScriptedChatModel mini(ScriptedChatModel model) {
        return model
                .rule("triage", r -> r.mentions("Classify the support ticket"), r -> LabReply.text(triage(tagged(r, "message"))))
                .rule("stock: nudged", r -> r.hasToolResult() && r.user().contains("empty reply"),
                        r -> LabReply.text("Yes. " + r.toolResults()))
                .rule("stock: silent after tool", r -> r.hasToolResult() && r.mentions("in stock"), r -> LabReply.text(""))
                .rule("stock: call tool", r -> r.mentions("in stock"),
                        r -> LabReply.callTool("stockLevel", "{\"sku\":\"" + sku(r.user()) + "\"}"))
                .rule("short reply", r -> r.mentions("Write a short reply"), r -> LabReply.text(shortReply(tagged(r, "message"))))
                .rule("reply (basic)", r -> r.mentions("Write a reply to the customer"), r -> LabReply.text(
                        "{\"subject\":\"We have your message\",\"body\":\"Thank you for contacting NorthWind. "
                                + "A member of our team will reply within one working day.\"}"));
    }

    /** Mid-priced all-rounder: replies, structured product data, tool use (Chapters 71-79). */
    public static ScriptedChatModel standard(ScriptedChatModel model) {
        return model
                .rule("reply", r -> r.mentions("Write a reply to the customer"), r -> LabReply.text(reply(r, false)))
                .rule("short reply", r -> r.mentions("Write a short reply"), r -> LabReply.text(shortReply(tagged(r, "message"))))
                .rule("product summary", r -> r.mentions("Summarise the product"), r -> LabReply.text(productSummary()))
                .rule("highlights stream", r -> r.mentions("List the product highlights"), r -> LabReply.text("""
                        {"sku":"TENT-2P","name":"Ridgeline 2-Person Tent","oneLiner":"Light and quick to pitch","highlights":["2.1 kg"]}
                        {"sku":"STOVE-1","name":"Trailhead Stove","oneLiner":"Boils a litre in four minutes","highlights":["piezo ignition"]}
                        {"sku":"BAG-15","name":"Summit 15 Sleeping Bag","oneLiner":"Comfort rated to 15 degrees","highlights":["840 g"]}
                        """))
                .rule("order: answer", r -> r.hasToolResult() && r.mentions("Where is my order"),
                        r -> LabReply.text("Here is the latest on your order: " + r.toolResults()))
                .rule("order: call tool", r -> r.mentions("Where is my order"),
                        r -> LabReply.callTool("orderStatus", "{\"orderNumber\":\"" + orderNumber(r.user()) + "\"}"));
    }

    /** Expensive and careful; shows its reasoning in think tags (Chapters 71 and 78). */
    public static ScriptedChatModel reasoner(ScriptedChatModel model) {
        return model
                .rule("reply", r -> r.mentions("Write a reply to the customer"), r -> LabReply.text(
                        "<think>The customer mentions an injury, so I must not admit liability or promise compensation. "
                                + "Apologise, ask for details and route to the safety team.</think>\n" + reply(r, true)))
                .rule("recommend: impossible", r -> r.mentions("Recommend one tent") && r.user().contains("under $20"),
                        r -> LabReply.text("<think>The cheapest tent in the catalogue costs $149, so nothing is under $20. "
                                + "I should not invent a product.</think>\n"
                                + "{\"failure\":\"No tent costs under $20; the cheapest is $149.\"}"))
                .rule("recommend", r -> r.mentions("Recommend one tent"), r -> LabReply.text(
                        "<analysis>Two-person, under 2.5 kg: Ridgeline 2P (2.1 kg, $199) and Basecamp 2 (2.9 kg, $149). "
                                + "Only the Ridgeline meets the weight limit.</analysis>\n"
                                + "<scratchpad>check stock later</scratchpad>\n"
                                + "{\"success\":{\"sku\":\"TENT-2P\",\"name\":\"Ridgeline 2-Person Tent\",\"oneLiner\":\"The only 2-person tent under 2.5 kg\",\"highlights\":[\"2.1 kg\",\"$199\"]}}"));
    }

    // ---- helpers -------------------------------------------------------------------------------

    private static String triage(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("lawyer") || m.contains("injur") || m.contains("burn")) {
            return json("COMPLAINT", true, "Mentions an injury or legal action");
        }
        if (m.contains("return") || m.contains("refund")) {
            return json("RETURN", false, "Wants to send an item back");
        }
        if (m.contains("order") || m.contains("nw-")) {
            return json("ORDER", false, "Asks about an existing order");
        }
        return json("PRODUCT", false, "General product question");
    }

    private static String json(String category, boolean legalRisk, String reason) {
        return "{\"category\":\"" + category + "\",\"legalRisk\":" + legalRisk + ",\"reason\":\"" + reason + "\"}";
    }

    private static String reply(LabRequest r, boolean careful) {
        String message = tagged(r, "message");
        String subject = careful ? "We are sorry - our safety team will contact you" : "Re: your NorthWind enquiry";
        String body = careful
                ? "We are very sorry to hear what happened. Our product safety team will contact you within one working day."
                : "Thank you for getting in touch about: " + escape(firstWords(message, 8)) + ". We will help you right away.";
        return "{\"subject\":\"" + subject + "\",\"body\":\"" + body + "\"}";
    }

    private static String shortReply(String message) {
        return "Thanks for your message. We have checked your order and it left our warehouse this morning. "
                + "You will receive tracking details by email within the hour.";
    }

    private static String productSummary() {
        return "{\"sku\":\"TENT-2P\",\"name\":\"Ridgeline 2-Person Tent\",\"oneLiner\":\"A light tent that pitches in five minutes\","
                + "\"highlights\":[\"2.1 kg\",\"3,000 mm flysheet\",\"two doors\"]}";
    }

    static String tagged(LabRequest r, String tag) {
        // the innermost block: the instructions may mention "<message> tags" before the real block
        Matcher m = Pattern.compile("<" + tag + ">\\s*((?:(?!<" + tag + ">).)*?)\\s*</" + tag + ">", Pattern.DOTALL)
                .matcher(r.system() + "\n" + r.user());
        return m.find() ? m.group(1) : r.user();
    }

    private static String sku(String text) {
        Matcher m = Pattern.compile("\\(([A-Z]+-[0-9A-Z]+)\\)").matcher(text);
        return m.find() ? m.group(1) : "UNKNOWN";
    }

    private static String orderNumber(String text) {
        Matcher m = Pattern.compile("NW-\\d{4,6}").matcher(text);
        return m.find() ? m.group() : "UNKNOWN";
    }

    private static String firstWords(String text, int n) {
        String[] words = text.trim().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOf(words, Math.min(n, words.length)));
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
