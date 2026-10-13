package com.northwind.advanced.migration;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.advanced.migration.ResearchBriefAgent.Brief;
import com.northwind.advanced.migration.ResearchBriefAgent.ResearchNotes;
import com.northwind.advanced.migration.ResearchBriefAgent.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 119: the migrated two-step crew runs in the order its types imply. */
@SpringBootTest
@ActiveProfiles("lab")
class MigrationTest {

    @Autowired AgentPlatform platform;

    @Test
    void theTypesDecideTheOrder() {
        var process = AgentInvocation.create(platform, Brief.class).run(new Topic("winter tents"));
        System.out.println("actions: " + process.getHistory().stream()
                .map(h -> h.getActionName().replaceAll("^.*\\.", "")).toList());
        System.out.println("notes: " + process.last(ResearchNotes.class).points());
        System.out.println("brief: " + process.last(Brief.class));
        assertThat(process.last(Brief.class).title()).isEqualTo("Choosing a winter tent");
    }

    @Test
    void existingResearchIsReused() {
        var notes = new ResearchNotes(java.util.List.of("Down loses warmth when wet", "Synthetic dries faster"));
        var process = AgentInvocation.create(platform, Brief.class).run(new Topic("sleeping bags"), notes);
        System.out.println("given notes, actions: " + process.getHistory().stream()
                .map(h -> h.getActionName().replaceAll("^.*\\.", "")).toList());
        assertThat(process.getHistory()).hasSize(1);
    }
}
