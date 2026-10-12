package com.northwind.enterprise.domain;

/** P14: what flows through the platform. */
public final class Requests {

    private Requests() {
    }

    /** A customer's message. The customer id comes from authentication, never from the message. */
    public record CustomerRequest(String customerId, String text) {
    }

    public enum Route { SUPPORT, BILLING, RESEARCH, HUMAN }

    /** The router's decision: which specialist handles the request, and why. */
    public record RouteDecision(Route route, String reason) {
    }

    public record SupportAnswer(String text) {
    }

    public record BillingAnswer(String text) {
    }

    public record ResearchAnswer(String text) {
    }
}
