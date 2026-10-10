package com.northwind.springbasics.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records every priced order. The pricing service does not know this class exists. */
@Component
public class PricingAuditListener {

    private final List<OrderPricedEvent> audited = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(OrderPricedEvent event) {
        audited.add(event);
        System.out.printf("[audit] %s priced at %s%n", event.orderNumber(), event.total());
    }

    public List<OrderPricedEvent> audited() {
        return List.copyOf(audited);
    }
}
