package com.northwind.advanced.tooling;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.advanced.migration.ResearchBriefAgent.Brief;
import com.northwind.advanced.migration.ResearchBriefAgent.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 114: prompts and responses in the log, for one run only. */
@SpringBootTest
@ActiveProfiles("lab")
class VerboseRunTest {

    @Autowired AgentPlatform platform;

    @Test
    void oneVerboseRun() {
        var defaults = new ProcessOptions();
        var options = defaults.withVerbosity(defaults.getVerbosity()
                .withShowPrompts(true)
                .withShowLlmResponses(true)
                .withShowPlanning(true));
        Brief brief = AgentInvocation.create(platform, Brief.class)
                .withProcessOptions(options)
                .invoke(new Topic("winter tents"));
        assertThat(brief.title()).isNotBlank();
    }
}
