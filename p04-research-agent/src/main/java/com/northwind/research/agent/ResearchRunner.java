package com.northwind.research.agent;

import com.embabel.agent.api.common.PlannerType;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.Goal;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.support.NirvanaKt;
import com.northwind.research.domain.Research.ProductQuestion;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Runs the same agent under different planners, to compare their behaviour. */
@Service
public class ResearchRunner {

    private final AgentPlatform platform;

    public ResearchRunner(AgentPlatform platform) {
        this.platform = platform;
    }

    /** GOAP (default): the cheapest path to the goal. Optional research is skipped. */
    public AgentProcess withGoap(ProductQuestion question) {
        return AgentInvocation.create(platform, com.northwind.research.domain.Research.ResearchBrief.class)
                .run(question);
    }

    /**
     * HYBRID: utility-style picking by net value (research first), stopping as soon as the real
     * goal is satisfied. Needs the NIRVANA goal alongside the real one, so we derive an agent
     * from the deployed metadata with both goals.
     */
    public AgentProcess withHybrid(ProductQuestion question) {
        Agent deployed = platform.agents().stream()
                .filter(a -> a.getName().equals("ProductResearchAgent"))
                .findFirst().orElseThrow();

        Set<Goal> goals = new HashSet<>(deployed.getGoals());
        goals.add(NirvanaKt.getNIRVANA());

        Agent hybrid = new Agent(deployed.getName(), deployed.getProvider(), "1.0.0",
                deployed.getDescription(), goals, deployed.getActions(), deployed.getConditions(),
                deployed.getStuckHandler());

        return platform.runAgentFrom(hybrid,
                new ProcessOptions().withPlannerType(PlannerType.HYBRID),
                Map.of("it", question));
    }
}
