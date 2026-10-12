package com.northwind.probes;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.core.AgentPlatform;

/**
 * Chapter 95: two tiny agents for observing threads. They live outside the application's packages, so
 * component scanning ignores them; tests deploy them explicitly.
 */
public final class ProbeAgents {

    private ProbeAgents() {
    }

    public static void deploy(AgentPlatform platform) {
        var reader = new AgentMetadataReader();
        if (platform.agents().stream().noneMatch(a -> a.getName().equals("ThreadProbeAgent"))) {
            platform.deploy(reader.createAgentMetadata(new ThreadProbeAgent()));
            platform.deploy(reader.createAgentMetadata(new ParallelProbeAgent()));
        }
    }

    public record Probe(String label) {
    }

    public record ThreadSeen(String thread) {
    }

    @Agent(description = "Records which thread runs its action")
    public static class ThreadProbeAgent {

        @AchievesGoal(description = "Record the thread")
        @Action
        public ThreadSeen record(Probe probe) {
            return new ThreadSeen(Thread.currentThread().getName());
        }
    }

    public record Start(String id) {
    }

    public record Stock(String thread) {
    }

    public record Price(String thread) {
    }

    public record Both(Stock stock, Price price) {
    }

    @Agent(description = "Two independent slow lookups, then a combination")
    public static class ParallelProbeAgent {

        @Action
        public Stock stock(Start start) {
            pause();
            return new Stock(Thread.currentThread().getName());
        }

        @Action
        public Price price(Start start) {
            pause();
            return new Price(Thread.currentThread().getName());
        }

        @AchievesGoal(description = "Combine stock and price")
        @Action
        public Both both(Stock stock, Price price) {
            return new Both(stock, price);
        }

        private static void pause() {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
