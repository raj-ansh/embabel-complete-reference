package com.northwind.returns.service;

import com.northwind.returns.domain.Order;
import com.northwind.returns.domain.ReturnDecision.Outcome;
import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The business rules are plain Java, so they get thorough, fast, deterministic tests. */
class ReturnsPolicyTest {

    private final Clock today = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
    private final ReturnsPolicy policy = new ReturnsPolicy(today);

    @ParameterizedTest(name = "{0} after {1} days, final sale {2} -> {3}")
    @CsvSource({
            "DAMAGED,       5,  false, APPROVED",
            "CHANGED_MIND,  5,  false, APPROVED",
            "CHANGED_MIND, 45,  false, REJECTED",
            "DAMAGED,      45,  false, APPROVED",
            "DOES_NOT_FIT,  5,  true,  REJECTED",
            "WRONG_ITEM,    5,  true,  APPROVED"
    })
    void appliesThePolicy(ReturnReason reason, int daysAgo, boolean finalSale, Outcome expected) {
        var order = new Order("NW-1", "Asha", LocalDate.of(2026, 10, 7).minusDays(daysAgo),
                List.of(new Order.Line("SKU-1", "Item", new BigDecimal("50.00"), finalSale)));

        var decision = policy.evaluate(new ReturnRequest("NW-1", "SKU-1", reason, null), order);

        assertThat(decision.outcome()).isEqualTo(expected);
    }
}
