package com.northwind.planning.utility;

import com.embabel.agent.api.common.scope.AgentScopeBuilder;
import com.embabel.agent.api.invocation.UtilityInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import org.springframework.stereotype.Service;

/** Runs the review actions with the Utility planner, without declaring an agent. */
@Service
public class ReviewEventHandler {

    private final AgentPlatform platform;
    private final ReviewEventActions actions;

    public ReviewEventHandler(AgentPlatform platform, ReviewEventActions actions) {
        this.platform = platform;
        this.actions = actions;
    }

    public AgentProcess onNewReview(ReviewEventActions.NewReview review) {
        return UtilityInvocation.on(platform)
                .withScope(AgentScopeBuilder.fromInstance(actions))
                .withAgentName("review-event-handler")
                .terminateWhenStuck()                  // stop once no action is applicable
                .run(review);
    }
}
