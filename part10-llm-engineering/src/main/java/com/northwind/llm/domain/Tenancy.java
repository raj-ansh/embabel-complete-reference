package com.northwind.llm.domain;

/** Chapter 80: who a request is for. Put on the blackboard by the application, never by the model. */
public final class Tenancy {

    private Tenancy() {
    }

    public record TenantContext(String tenant, String userId) {
    }

    public record TenantQuestion(String text) {
    }

    public record TenantAnswer(String text) {
    }
}
