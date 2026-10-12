package com.northwind.production.unit;

import com.northwind.production.agent.RemedyPolicy;
import com.northwind.production.domain.Issues.Category;
import com.northwind.production.domain.Issues.OrderFacts;
import com.northwind.production.domain.Issues.Remedy;
import com.northwind.production.domain.Issues.Triage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 90: the rules that decide money are plain code, so every branch and boundary gets a test. */
class RemedyPolicyTest {

    @ParameterizedTest(name = "{0}, worth {1}, {2} -> {3} {4}")
    @CsvSource({
            "DAMAGED,    150.00, DELIVERED,  REFUND,   150.00",
            "DAMAGED,    150.01, DELIVERED,  ESCALATE, 0",
            "WRONG_ITEM, 59.00,  DELIVERED,  REPLACE,  0",
            "LATE,       349.00, IN_TRANSIT, REFUND,   4.99",
            "LATE,       129.00, DELIVERED,  ESCALATE, 0",
            "OTHER,      10.00,  DELIVERED,  ESCALATE, 0"})
    void decides(Category category, BigDecimal value, String status, Remedy remedy, BigDecimal amount) {
        var resolution = RemedyPolicy.decide(new Triage(category, false),
                new OrderFacts("NW-1", "C-1", status, value));

        assertThat(resolution.remedy()).isEqualTo(remedy);
        assertThat(resolution.amount()).isEqualByComparingTo(amount);
    }
}
