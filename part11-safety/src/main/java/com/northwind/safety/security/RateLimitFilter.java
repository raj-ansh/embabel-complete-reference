package com.northwind.safety.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chapter 84: a token bucket per authenticated user. Model calls are expensive, so one user (or one
 * stolen token) must not be able to consume the platform's capacity or budget.
 * Single-instance only: with several instances, keep the buckets in a shared store such as Redis.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final int perMinute;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(int perMinute) {
        this.perMinute = perMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user != null && user.isAuthenticated()
                && !buckets.computeIfAbsent(user.getName(), name -> new Bucket(perMinute)).tryTake()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Too many requests. Please wait a minute.");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Refills continuously at perMinute tokens per minute, up to perMinute. */
    private static final class Bucket {
        private final int capacity;
        private double tokens;
        private long last = System.nanoTime();

        Bucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
        }

        synchronized boolean tryTake() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - last) / 60e9 * capacity);
            last = now;
            if (tokens < 1) {
                return false;
            }
            tokens -= 1;
            return true;
        }
    }
}
