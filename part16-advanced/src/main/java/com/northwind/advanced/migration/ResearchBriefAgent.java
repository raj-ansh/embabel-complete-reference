package com.northwind.advanced.migration;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;

import java.util.List;

/**
 * Chapter 119: a two-agent "crew" (a researcher's task, then a writer's task that uses its output) migrated to
 * Embabel. The roles become actions, the tasks' expected outputs become types, and the order is no longer
 * written down: the planner derives it from the types.
 */
@Agent(description = "Researches a gear topic and writes a short buyer's brief")
public class ResearchBriefAgent {

    public record Topic(String subject) {
    }

    public record ResearchNotes(List<String> points) {
    }

    public record Brief(String title, String body) {
    }

    @Action(description = "Research the topic")
    public ResearchNotes research(Topic topic, OperationContext context) {
        return context.ai().withLlmByRole("standard").withId("researcher")
                .createObject("""
                        You are NorthWind's gear researcher. List three facts a buyer should know about: %s"""
                        .formatted(topic.subject()), ResearchNotes.class);
    }

    @AchievesGoal(description = "A short buyer's brief on the topic")
    @Action(description = "Write the brief from the research")
    public Brief write(Topic topic, ResearchNotes notes, OperationContext context) {
        return context.ai().withLlmByRole("standard").withId("writer")
                .createObject("""
                        You are NorthWind's writer. Write a short brief on %s using only these facts:
                        %s""".formatted(topic.subject(), String.join("\n", notes.points())), Brief.class);
    }
}
