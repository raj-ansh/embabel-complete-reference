package com.northwind.agents;

import com.embabel.agent.core.AgentPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Logs what the platform deployed, so you can see your agents were discovered at startup. */
@Component
public class PlatformReport implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformReport.class);

    private final AgentPlatform platform;

    public PlatformReport(AgentPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Deployed {} agent(s):", platform.agents().size());
        platform.agents().forEach(agent -> log.info("  - {}: {} ({} action(s), {} goal(s))",
                agent.getName(), agent.getDescription(),
                agent.getActions().size(), agent.getGoals().size()));
    }
}
