package com.northwind.agents.smoke;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;

/**
 * The smallest useful agent: one action that turns user input into a typed Greeting.
 * Its only job in this chapter is to prove that the platform, the model and the build all work.
 */
@Agent(description = "Greets a person named in the user's message")
public class SmokeTestAgent {

    @AchievesGoal(description = "A friendly greeting has been written")
    @Action
    public Greeting greet(UserInput input, OperationContext context) {
        return context.ai()
                .withDefaultLlm()
                .createObject("""
                        Find the name of the person in the message below and write them
                        a one-sentence welcome to NorthWind Outfitters.

                        <message>
                        %s
                        </message>""".formatted(input.getContent()), Greeting.class);
    }
}
