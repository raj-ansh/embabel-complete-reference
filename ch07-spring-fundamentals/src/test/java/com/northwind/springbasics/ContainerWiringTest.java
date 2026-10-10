package com.northwind.springbasics;

import com.northwind.springbasics.config.AppConfig;
import com.northwind.springbasics.domain.Order;
import com.northwind.springbasics.event.PricingAuditListener;
import com.northwind.springbasics.service.OrderPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** An integration test: the real container wires the beans, applies the aspect and delivers events. */
@SpringJUnitConfig(AppConfig.class)
class ContainerWiringTest {

    @Autowired
    OrderPricingService pricing;

    @Autowired
    PricingAuditListener audit;

    @Test
    void serviceIsProxiedAndEventsAreDelivered() {
        assertThat(AopUtils.isAopProxy(pricing)).isTrue();          // the TimingAspect wrapped it

        pricing.price(new Order("NW-2", "BOOT-W9", 1, new BigDecimal("120.00")));

        assertThat(audit.audited()).anyMatch(e -> e.orderNumber().equals("NW-2"));
    }
}
