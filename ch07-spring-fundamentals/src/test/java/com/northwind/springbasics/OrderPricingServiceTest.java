package com.northwind.springbasics;

import com.northwind.springbasics.domain.Order;
import com.northwind.springbasics.event.OrderPricedEvent;
import com.northwind.springbasics.service.OrderPricingService;
import com.northwind.springbasics.service.SeasonalDiscountPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A plain unit test: constructor injection means no container is needed. */
class OrderPricingServiceTest {

    private final Clock inMay = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void appliesSeasonalTentDiscount() {
        List<Object> published = new ArrayList<>();
        var service = new OrderPricingService(new SeasonalDiscountPolicy(inMay), published::add);

        BigDecimal total = service.price(new Order("NW-1", "TENT-2P", 2, new BigDecimal("100.00")));

        assertThat(total).isEqualByComparingTo("180.00");
        assertThat(published).containsExactly(new OrderPricedEvent("NW-1", new BigDecimal("180.00")));
    }
}
