package com.northwind.research;

import com.embabel.agent.core.ActionInvocation;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.research.agent.ResearchRunner;
import com.northwind.research.domain.Research.ProductQuestion;
import com.northwind.research.domain.Research.ResearchBrief;
import com.northwind.research.domain.Research.ResearchBrief.Recommendation;
import com.northwind.research.sources.ResearchSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The same agent, two planners, two very different behaviours. */
class PlannerComparisonTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    ResearchRunner runner;

    @Autowired
    ResearchSources sources;

    private final ProductQuestion question = new ProductQuestion("Glacier Insulated Jacket", "outerwear");

    @BeforeEach
    void stubSynthesis() {
        sources.reset();
        whenCreateObject(p -> p.contains("merchandising analyst"), ResearchBrief.class)
                .thenAnswer(inv -> {
                    String prompt = inv.getArgument(0).toString();
                    boolean hasEvidence = !prompt.contains("(none)");
                    return new ResearchBrief(hasEvidence ? Recommendation.TRIAL_ORDER : Recommendation.NEED_MORE_DATA,
                            hasEvidence ? "Good reviews, healthy margin, peak season ahead" : "No evidence",
                            List.of());
                });
    }

    private static List<String> actions(AgentProcess process) {
        return process.getHistory().stream().map(ActionInvocation::getActionName)
                .map(n -> n.substring(n.lastIndexOf('.') + 1)).toList();
    }

    @Test
    void goapTakesTheCheapestPathAndSkipsOptionalResearch() {
        AgentProcess process = runner.withGoap(question);

        System.out.println("GOAP   " + actions(process));
        assertThat(actions(process)).containsExactly("synthesise");
        assertThat(sources.lookups()).isZero();
        assertThat(process.resultOfType(ResearchBrief.class).recommendation()).isEqualTo(Recommendation.NEED_MORE_DATA);
    }

    @Test
    void hybridResearchesByValueThenSynthesisesAndStops() {
        AgentProcess process = runner.withHybrid(question);

        System.out.println("HYBRID " + actions(process));
        assertThat(actions(process)).containsExactly(
                "supplierTerms", "reviewSignal", "competitorPricing", "seasonalDemand", "synthesise");
        assertThat(sources.lookups()).isEqualTo(4);
        assertThat(process.getStatus()).isEqualTo(AgentProcessStatusCode.COMPLETED);
        assertThat(process.resultOfType(ResearchBrief.class).recommendation()).isEqualTo(Recommendation.TRIAL_ORDER);
    }
}
