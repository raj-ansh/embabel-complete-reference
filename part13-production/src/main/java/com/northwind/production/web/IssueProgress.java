package com.northwind.production.web;

import com.embabel.agent.api.event.AbstractAgentProcessEvent;
import com.embabel.agent.api.event.ActionExecutionStartEvent;
import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.api.event.AgentProcessFinishedEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chapters 97 and 98: turns Embabel's events into progress a customer can understand, and streams it as
 * server-sent events. Steps are kept per process, so a client that subscribes late still sees all of them.
 * Single instance only: with several instances, publish the steps through a shared broker instead.
 */
@Component
public class IssueProgress implements AgenticEventListener {

    private static final Map<String, String> STEPS = Map.of(
            "lookUp", "Checking your order",
            "triage", "Reading your message",
            "decide", "Deciding what we can do",
            "reply", "Writing your reply");

    private record Stream(List<String> steps, List<SseEmitter> emitters, boolean[] done) {
    }

    private final Map<String, Stream> streams = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Stream> eldest) {
            return size() > 1000;                                  // keep memory bounded
        }
    });

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        if (!(event instanceof AbstractAgentProcessEvent processEvent)
                || !processEvent.getAgentProcess().getAgent().getName().equals("IssueResolutionAgent")) {
            return;
        }
        String id = event.getProcessId();
        if (event instanceof ActionExecutionStartEvent start) {
            String action = start.getAction().getName();
            String step = STEPS.get(action.substring(action.lastIndexOf('.') + 1));
            if (step != null) {
                publish(id, step, false);
            }
        } else if (event instanceof AgentProcessFinishedEvent) {
            publish(id, "done", true);
        }
    }

    public SseEmitter subscribe(String processId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Stream stream = stream(processId);
        synchronized (stream) {
            for (String step : stream.steps()) {
                send(emitter, step);
            }
            if (stream.done()[0]) {
                emitter.complete();
            } else {
                stream.emitters().add(emitter);
                emitter.onCompletion(() -> stream.emitters().remove(emitter));
                emitter.onTimeout(() -> stream.emitters().remove(emitter));
            }
        }
        return emitter;
    }

    private void publish(String processId, String step, boolean last) {
        Stream stream = stream(processId);
        synchronized (stream) {
            stream.steps().add(step);
            stream.emitters().forEach(emitter -> send(emitter, step));
            if (last) {
                stream.done()[0] = true;
                stream.emitters().forEach(SseEmitter::complete);
            }
        }
    }

    private Stream stream(String processId) {
        return streams.computeIfAbsent(processId,
                id -> new Stream(new ArrayList<>(), new CopyOnWriteArrayList<>(), new boolean[1]));
    }

    private static void send(SseEmitter emitter, String step) {
        try {
            emitter.send(SseEmitter.event().name("step").data(step));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }
}
