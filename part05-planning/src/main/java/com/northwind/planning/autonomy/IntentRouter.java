package com.northwind.planning.autonomy;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.api.common.autonomy.GoalChoiceApprover;
import com.embabel.agent.api.common.autonomy.GoalSelectionOptions;
import com.embabel.agent.api.common.autonomy.ProcessExecutionException;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Chapter 36: letting a model choose the agent (closed mode) or the goal (open mode). */
@Service
public class IntentRouter {

    private final Autonomy autonomy;
    private final AgentPlatform platform;

    public IntentRouter(Autonomy autonomy, AgentPlatform platform) {
        this.autonomy = autonomy;
        this.platform = platform;
    }

    /** Closed mode: rank agents against the text, run the best one in isolation. */
    public AgentProcessExecution routeToAgent(String text) throws ProcessExecutionException {
        return autonomy.chooseAndRunAgent(text, new ProcessOptions());
    }

    /** Open mode: rank goals across all agents, then assemble actions from anywhere to reach it. */
    public AgentProcessExecution accomplishGoal(String text) throws ProcessExecutionException {
        return autonomy.chooseAndAccomplishGoal(
                new ProcessOptions(),
                GoalChoiceApprover.Companion.getAPPROVE_ALL(),   // Kotlin companion: see the note in Chapter 36
                platform,
                Map.of("userInput", new UserInput(text)),
                new GoalSelectionOptions());
    }
}
