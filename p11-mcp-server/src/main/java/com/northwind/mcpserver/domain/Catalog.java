package com.northwind.mcpserver.domain;

import java.util.List;

/** P11: the inputs and results of NorthWind's published capabilities. */
public final class Catalog {

    private Catalog() {
    }

    public record OrderQuery(String orderNumber) {
    }

    public record OrderStatus(String orderNumber, String status, String expectedDelivery) {
    }

    public record TripQuestion(String trip) {
    }

    public record ProductAdvice(List<String> skus, String reason) {
    }
}
