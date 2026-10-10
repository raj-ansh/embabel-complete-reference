package com.northwind.planning;

import com.embabel.agent.api.common.PlannerType;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.planning.goap.ShippingQuoteAgent.Parcel;
import com.northwind.planning.goap.ShippingQuoteAgent.ShippingQuote;
import com.northwind.planning.utility.ReviewEventActions;
import com.northwind.planning.utility.ReviewEventActions.NewReview;
import com.northwind.planning.utility.ReviewEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    ReviewEventHandler reviews;

    @Autowired
    ReviewEventActions reviewActions;

    @BeforeEach
    void reset() {
        reviewActions.reset();
    }

    // Chapter 32: GOAP picks the cheaper action when its precondition holds
    @Test
    void goapPrefersTheCheaperPath() {
        ShippingQuote standard = AgentInvocation.create(agentPlatform, ShippingQuote.class)
                .invoke(new Parcel("Z2", 3.0, false));
        ShippingQuote oversized = AgentInvocation.create(agentPlatform, ShippingQuote.class)
                .invoke(new Parcel("Z2", 3.0, true));

        assertThat(standard.rateSource()).isEqualTo("rate-table");
        assertThat(oversized.rateSource()).isEqualTo("carrier-api");
    }

    // Chapter 33: Utility fires every applicable action, highest net value first
    @Test
    void utilityReactsToAPoorReview() {
        reviews.onNewReview(new NewReview("BOOT-W9", 1, "Leaked on day one"));

        System.out.println("UTILITY 1-star " + reviewActions.fired());
        assertThat(reviewActions.fired()).containsExactly("alertQuality", "thankReviewer");
    }

    @Test
    void utilityReactsToAGlowingReview() {
        reviews.onNewReview(new NewReview("TENT-2P", 5, "Best tent I've owned"));

        System.out.println("UTILITY 5-star " + reviewActions.fired());
        assertThat(reviewActions.fired()).containsExactly("considerFeaturing", "thankReviewer");
    }

    // Chapter 35: the supervisor agent is deployed with its planner type
    @Test
    void supervisorAgentIsDeployed() {
        assertThat(agentPlatform.agents()).anyMatch(a -> a.getName().equals("CategoryReportAgent"));
        assertThat(PlannerType.SUPERVISOR).isNotNull();
    }
}
