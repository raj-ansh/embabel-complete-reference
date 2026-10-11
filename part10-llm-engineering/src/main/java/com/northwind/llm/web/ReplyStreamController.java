package com.northwind.llm.web;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.api.streaming.StreamingPromptRunnerBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chapter 77: a reply streamed to the browser as server-sent events. The client sees the first words
 * while the model is still writing the rest.
 */
@RestController
@RequestMapping("/api/replies")
public class ReplyStreamController {

    private static final int MAX_MESSAGE_LENGTH = 2_000;

    private final Ai ai;

    public ReplyStreamController(Ai ai) {
        this.ai = ai;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> stream(@RequestParam String message) {
        if (message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest().build();
        }
        PromptRunner runner = ai.withLlmByRole("cheap");
        String prompt = """
                Write a short reply to the customer message between the <message> tags.
                <message>
                %s
                </message>""".formatted(message);

        Flux<ServerSentEvent<String>> events = runner.supportsStreaming()
                ? new StreamingPromptRunnerBuilder(runner).streaming().withPrompt(prompt).generateStream()
                        .map(chunk -> ServerSentEvent.builder(chunk).event("chunk").build())
                : Flux.just(ServerSentEvent.builder(runner.generateText(prompt)).event("chunk").build()); // no streaming: one event

        return ResponseEntity.ok(events
                .concatWith(Flux.just(ServerSentEvent.builder("").event("done").build()))
                .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Sorry, something went wrong. Please try again.")
                        .event("error").build())));
    }
}
