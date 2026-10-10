package com.northwind.tools.security;

import com.embabel.agent.api.tool.DelegatingTool;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.ToolCallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chapter 47: a security decorator for any tool. It wraps the tool without changing it and adds
 * - authorisation: the caller (from tool call context, never from the model) must hold a required role
 * - a call budget per customer per hour, so a looping or manipulated model cannot hammer a write
 * - an audit line per call, with the model's arguments truncated
 * - safe errors: exceptions become a short message for the model; details stay in the log
 */
public final class GuardedTool implements DelegatingTool {

    private static final Logger log = LoggerFactory.getLogger("northwind.audit");

    private final Tool delegate;
    private final String requiredRole;
    private final int maxCallsPerHour;
    private final Clock clock;
    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();

    public GuardedTool(Tool delegate, String requiredRole, int maxCallsPerHour) {
        this(delegate, requiredRole, maxCallsPerHour, Clock.systemUTC());
    }

    public GuardedTool(Tool delegate, String requiredRole, int maxCallsPerHour, Clock clock) {
        this.delegate = delegate;
        this.requiredRole = requiredRole;
        this.maxCallsPerHour = maxCallsPerHour;
        this.clock = clock;
    }

    @Override
    public Tool getDelegate() {
        return delegate;
    }

    @Override
    public Tool.Definition getDefinition() {
        return delegate.getDefinition();
    }

    @Override
    public Tool.Result call(String input) {
        return call(input, ToolCallContext.EMPTY);
    }

    @Override
    public Tool.Result call(String input, ToolCallContext context) {
        String tool = delegate.getDefinition().getName();
        String customerId = context.get("customerId");
        Set<String> roles = context.getOrDefault("roles", Set.of());

        if (customerId == null || !roles.contains(requiredRole)) {
            log.warn("DENIED tool={} customer={} reason=missing-role:{}", tool, customerId, requiredRole);
            return Tool.Result.text("This action is not available for your account.");
        }
        String window = "@" + clock.instant().truncatedTo(ChronoUnit.HOURS);
        calls.keySet().removeIf(k -> !k.endsWith(window));          // forget earlier hours
        int n = calls.computeIfAbsent(customerId + window, k -> new AtomicInteger()).incrementAndGet();
        if (n > maxCallsPerHour) {
            log.warn("DENIED tool={} customer={} reason=call-budget calls={}", tool, customerId, n);
            return Tool.Result.text("Too many requests of this kind. Please contact support.");
        }
        log.info("CALL tool={} customer={} args={}", tool, customerId, truncate(input));
        try {
            Tool.Result result = delegate.call(input, context);
            log.info("OK tool={} customer={}", tool, customerId);
            return result;
        } catch (RuntimeException e) {
            log.error("FAILED tool={} customer={}", tool, customerId, e);
            return Tool.Result.error("The request could not be completed.");
        }
    }

    private static String truncate(String s) {
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
