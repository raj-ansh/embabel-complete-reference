package com.northwind.advanced.architecture;

import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.advanced.migration.ResearchBriefAgent.Brief;
import com.northwind.advanced.migration.ResearchBriefAgent.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 116: the platform's parts, and the events one agent run produces, in order. */
@SpringBootTest
@ActiveProfiles("lab")
@Import(ArchitectureTest.RecordEvents.class)
class ArchitectureTest {

    @TestConfiguration
    static class RecordEvents {
        static final List<AgentProcessEvent> EVENTS = new CopyOnWriteArrayList<>();
        static final List<String> THREADS = new CopyOnWriteArrayList<>();

        @Bean
        AgenticEventListener recordEvents() {
            return new AgenticEventListener() {
                @Override
                public void onProcessEvent(AgentProcessEvent event) {
                    EVENTS.add(event);
                    THREADS.add(Thread.currentThread().getName());
                }
            };
        }
    }

    @Autowired AgentPlatform platform;

    private static String simple(Object o) {
        return o == null ? "null" : o.getClass().getName().replaceAll("^.*\\.", "");
    }

    @Test
    void thePartsOfThePlatform() {
        var services = platform.getPlatformServices();
        System.out.println("AgentPlatform: " + simple(platform));
        System.out.println("LlmOperations: " + simple(services.getLlmOperations()));
        System.out.println("OperationScheduler: " + simple(services.getOperationScheduler()));
        System.out.println("AgentProcessRepository: " + simple(services.getAgentProcessRepository()));
        System.out.println("Asyncer: " + simple(services.getAsyncer()));
        System.out.println("TemplateRenderer: " + simple(services.getTemplateRenderer()));
        System.out.println("OutputChannel: " + simple(services.getOutputChannel()));
        System.out.println("ToolGroupResolver: " + simple(platform.getToolGroupResolver()));
        System.out.println("agents deployed: " + platform.agents().stream().map(a -> a.getName()).sorted().toList());
        assertThat(platform.agents()).isNotEmpty();
    }

    @Test
    void oneRunEventByEvent() {
        RecordEvents.EVENTS.clear();
        RecordEvents.THREADS.clear();
        String caller = Thread.currentThread().getName();
        var process = AgentInvocation.create(platform, Brief.class).run(new Topic("winter tents"));
        System.out.println("planner: " + simple(process.getPlanner()) + "; status: " + process.getStatus()
                + "; actions: " + process.getHistory().stream().map(h -> h.getActionName().replaceAll("^.*\\.", "")).toList());
        List<String> sequence = new ArrayList<>();
        for (AgentProcessEvent event : RecordEvents.EVENTS) {
            if (!event.getProcessId().equals(process.getId())) {
                continue;
            }
            String name = simple(event).replace("Event", "");
            if (sequence.isEmpty() || !sequence.getLast().equals(name)) {
                sequence.add(name);
            }
        }
        System.out.println("caller thread: " + caller + "; listener threads: " + RecordEvents.THREADS.stream()
                .map(t -> t.replaceAll("\\d+$", "N")).distinct().sorted().toList());
        System.out.println("events:");
        for (int i = 0; i < sequence.size(); i += 4) {
            System.out.println("  " + String.join(" > ", sequence.subList(i, Math.min(sequence.size(), i + 4))));
        }
        assertThat(sequence).contains("AgentProcessCompleted");
    }
}
