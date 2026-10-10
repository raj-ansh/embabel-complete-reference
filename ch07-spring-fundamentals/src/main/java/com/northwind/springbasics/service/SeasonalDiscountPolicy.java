package com.northwind.springbasics.service;

import com.northwind.springbasics.domain.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Month;

/** 10% off tents during the spring camping season. */
@Component
public class SeasonalDiscountPolicy implements DiscountPolicy {

    private final Clock clock;

    public SeasonalDiscountPolicy(Clock clock) {          // the Clock bean is injected
        this.clock = clock;
    }

    @Override
    public BigDecimal discountFor(Order order) {
        Month month = clock.instant().atZone(clock.getZone()).getMonth();
        boolean season = month == Month.APRIL || month == Month.MAY;
        if (season && order.sku().startsWith("TENT-")) {
            return order.subtotal().multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2);
    }
}
