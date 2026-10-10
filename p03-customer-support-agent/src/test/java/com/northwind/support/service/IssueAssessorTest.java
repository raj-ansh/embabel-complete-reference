package com.northwind.support.service;

import com.northwind.support.domain.SupportDomain.CustomerProfile;
import com.northwind.support.domain.SupportDomain.CustomerProfile.Tier;
import com.northwind.support.domain.SupportDomain.SupportRequest;
import com.northwind.support.domain.SupportDomain.SupportRequest.Category;
import com.northwind.support.domain.SupportDomain.SupportRequest.Urgency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IssueAssessorTest {

    private final IssueAssessor assessor = new IssueAssessor(new OrderLookup());
    private final CustomerProfile asha = new CustomerProfile("C-1001", "Asha", Tier.GOLD, 0);
    private final CustomerProfile mateo = new CustomerProfile("C-1002", "Mateo", Tier.STANDARD, 2);

    @Test
    void ownOrderFactsAreGathered() {
        var assessment = assessor.assess(request(Category.ORDER_STATUS, "NW-10442", Urgency.NORMAL), asha);

        assertThat(assessment.facts()).singleElement().asString().contains("SHIPPED");
        assertThat(assessment.escalate()).isFalse();
    }

    @Test
    void someoneElsesOrderLooksExactlyLikeAMissingOne() {
        var foreign = assessor.assess(request(Category.ORDER_STATUS, "NW-10442", Urgency.NORMAL), mateo);
        var missing = assessor.assess(request(Category.ORDER_STATUS, "NW-99999", Urgency.NORMAL), mateo);

        assertThat(foreign.facts()).singleElement().asString()
                .contains("No order NW-10442 was found").doesNotContain("SHIPPED", "189");
        assertThat(missing.facts()).singleElement().asString().contains("No order NW-99999 was found");
    }

    @Test
    void billingAlwaysGoesToAHuman() {
        assertThat(assessor.assess(request(Category.BILLING, null, Urgency.LOW), mateo).escalate()).isTrue();
    }

    @Test
    void urgentGoldMemberIsEscalated() {
        var assessment = assessor.assess(request(Category.PRODUCT_QUESTION, null, Urgency.HIGH), asha);

        assertThat(assessment.escalate()).isTrue();
        assertThat(assessment.escalationReason()).contains("Gold");
    }

    private static SupportRequest request(Category category, String order, Urgency urgency) {
        return new SupportRequest(category, order, null, "summary", urgency);
    }
}
