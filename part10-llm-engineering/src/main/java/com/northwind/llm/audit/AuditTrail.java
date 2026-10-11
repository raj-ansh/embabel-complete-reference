package com.northwind.llm.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chapter 79: an append-only record of what the model asked for and what happened. Production code
 * writes these to a table or an audit log service; this one keeps them in memory and logs them.
 */
@Component
public class AuditTrail {

    public record Entry(Instant at, String processId, String kind, String detail) {
        @Override
        public String toString() {
            return "%-10s %-12s %s".formatted(processId, kind, detail);
        }
    }

    private static final Logger log = LoggerFactory.getLogger("northwind.audit");

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void record(String processId, String kind, String detail) {
        var entry = new Entry(Instant.now(), processId, kind, detail);
        entries.add(entry);
        log.info("{}", entry);
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
