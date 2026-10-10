package com.northwind.support.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/** Stand-in for the help-desk system human agents work from. */
@Service
public class TicketService {

    private final AtomicInteger sequence = new AtomicInteger(5000);

    public String open(String customerId, String summary, String reason) {
        return "TCK-" + sequence.incrementAndGet();
    }
}
