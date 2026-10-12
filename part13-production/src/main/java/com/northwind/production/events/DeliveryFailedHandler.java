package com.northwind.production.events;

import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.service.IssueService;
import com.northwind.production.service.IssueService.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chapter 99: reacts to failed deliveries by running the issue agent and queueing a proactive message.
 * Events can arrive more than once, so each event id is handled once (in production: a unique constraint).
 */
@Component
public class DeliveryFailedHandler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryFailedHandler.class);

    private final IssueService issues;
    private final Outbox outbox;
    private final Set<String> handled = ConcurrentHashMap.newKeySet();

    public DeliveryFailedHandler(IssueService issues, Outbox outbox) {
        this.issues = issues;
        this.outbox = outbox;
    }

    @Async
    @EventListener
    public void on(DeliveryFailed event) {
        if (!handled.add(event.eventId())) {
            log.info("Event {} already handled", event.eventId());
            return;
        }
        var outcome = issues.resolve(new IssueReport(event.customerId(), event.orderNumber(),
                "Our carrier reported a failed delivery: " + event.carrierNote()));
        if (outcome.status() == Status.RESOLVED) {
            outbox.add(new Outbox.Message(event.customerId(), event.orderNumber(), outcome.reply().text()));
        } else {
            log.warn("Event {} needs a person: {}", event.eventId(), outcome.status());
        }
    }
}
