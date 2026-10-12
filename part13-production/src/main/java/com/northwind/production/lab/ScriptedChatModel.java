package com.northwind.production.lab;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A deterministic stand-in for a real chat model, so that Part XIII runs offline.
 * It is a genuine Spring AI {@link ChatModel}: Embabel builds the prompt, sends it here, executes
 * any tool calls it asks for and parses its replies exactly as it would for OpenAI or Anthropic.
 * Only the "intelligence" is scripted: replies come from rules, not from a neural network.
 */
public class ScriptedChatModel implements ChatModel {

    /** What the model was asked: system text, last user text, the conversation and the tools offered (name to description). */
    public record LabRequest(String system, String user, List<Message> messages, Map<String, String> tools) {

        public boolean hasToolResult() {
            return messages.stream().anyMatch(m -> m.getMessageType() == MessageType.TOOL);
        }

        public String toolResults() {
            return messages.stream()
                    .filter(m -> m instanceof ToolResponseMessage)
                    .flatMap(m -> ((ToolResponseMessage) m).getResponses().stream())
                    .map(ToolResponseMessage.ToolResponse::responseData)
                    .collect(Collectors.joining("\n"));
        }

        public boolean mentions(String text) {
            return system.contains(text) || user.contains(text);
        }
    }

    /** A scripted answer: text, or a request to call tools. */
    public record LabReply(String text, List<AssistantMessage.ToolCall> toolCalls) {

        public static LabReply text(String text) {
            return new LabReply(text, List.of());
        }

        public static LabReply callTool(String toolName, String jsonArguments) {
            var call = new AssistantMessage.ToolCall("call-" + UUID.randomUUID(), "function", toolName, jsonArguments);
            return new LabReply("", List.of(call));
        }
    }

    /** One rule: if {@code when} matches the request, {@code reply} produces the answer. */
    public record Rule(String name, Predicate<LabRequest> when, Function<LabRequest, LabReply> reply) {
    }

    private final String modelName;
    private final List<Rule> rules = new CopyOnWriteArrayList<>();
    private final List<Prompt> received = new CopyOnWriteArrayList<>();
    private volatile RuntimeException outage;
    private volatile java.time.Duration latency = java.time.Duration.ZERO;

    public ScriptedChatModel(String modelName) {
        this.modelName = modelName;
    }

    public ScriptedChatModel rule(String name, Predicate<LabRequest> when, Function<LabRequest, LabReply> reply) {
        rules.add(new Rule(name, when, reply));
        return this;
    }

    /** Simulates a provider outage: every call fails until {@link #recover()}. */
    public void failWith(RuntimeException failure) {
        this.outage = failure;
    }

    public void recover() {
        this.outage = null;
    }

    /** Simulates a provider's response time (Chapters 95 and 100). */
    public void setLatency(java.time.Duration latency) {
        this.latency = latency;
    }

    /** Every prompt this model has received, in order: what Embabel really sent. */
    public List<Prompt> received() {
        return List.copyOf(received);
    }

    public void clear() {
        received.clear();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        received.add(prompt);
        if (outage != null) {
            throw outage;
        }
        if (!latency.isZero()) {
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
        }
        LabReply reply = answer(prompt);
        var message = AssistantMessage.builder().content(reply.text()).toolCalls(reply.toolCalls()).build();
        return response(message, tokens(prompt.getContents()), Math.max(1, tokens(reply.text())));
    }

    /** Streams the reply a few words at a time; usage arrives with the last chunk, as with most providers. */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        received.add(prompt);
        if (outage != null) {
            return Flux.error(outage);
        }
        LabReply reply = answer(prompt);
        if (!reply.toolCalls().isEmpty()) {
            var message = AssistantMessage.builder().content("").toolCalls(reply.toolCalls()).build();
            return Flux.just(response(message, tokens(prompt.getContents()), 1));
        }
        List<String> chunks = chunks(reply.text(), 3);
        int promptTokens = tokens(prompt.getContents());
        int completionTokens = Math.max(1, tokens(reply.text()));
        List<ChatResponse> responses = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            boolean last = i == chunks.size() - 1;
            responses.add(response(new AssistantMessage(chunks.get(i)),
                    last ? promptTokens : 0, last ? completionTokens : 0));
        }
        return Flux.fromIterable(responses);
    }

    private LabReply answer(Prompt prompt) {
        var request = new LabRequest(
                prompt.getSystemMessages().stream().map(Message::getText).collect(Collectors.joining("\n")),
                prompt.getUserMessages().isEmpty() ? "" : prompt.getUserMessages().getLast().getText(),
                prompt.getInstructions(),
                tools(prompt));
        return rules.stream()
                .filter(rule -> rule.when().test(request))
                .findFirst()
                .map(rule -> rule.reply().apply(request))
                .orElse(LabReply.text("[" + modelName + "] I have no scripted answer for that."));
    }

    private static Map<String, String> tools(Prompt prompt) {
        Map<String, String> tools = new LinkedHashMap<>();
        if (prompt.getOptions() instanceof ToolCallingChatOptions options && options.getToolCallbacks() != null) {
            options.getToolCallbacks().forEach(t -> tools.put(t.getToolDefinition().name(), t.getToolDefinition().description()));
        }
        return tools;
    }

    private ChatResponse response(AssistantMessage message, int promptTokens, int completionTokens) {
        var metadata = ChatResponseMetadata.builder()
                .model(modelName)
                .usage(new DefaultUsage(promptTokens, completionTokens))
                .build();
        return new ChatResponse(List.of(new Generation(message)), metadata);
    }

    /** Roughly four characters per token, the usual rule of thumb for English text. */
    static int tokens(String text) {
        return text == null ? 0 : (text.length() + 3) / 4;
    }

    private static List<String> chunks(String text, int wordsPerChunk) {
        String[] words = text.split("(?<= )");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += wordsPerChunk) {
            chunks.add(String.join("", java.util.Arrays.copyOfRange(words, i, Math.min(words.length, i + wordsPerChunk))));
        }
        return chunks.isEmpty() ? List.of("") : chunks;
    }
}
