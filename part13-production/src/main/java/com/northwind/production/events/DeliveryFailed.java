package com.northwind.production.events;

/** Chapter 99: a carrier's report, as a domain event. The event id makes redelivery detectable. */
public record DeliveryFailed(String eventId, String orderNumber, String customerId, String carrierNote) {
}
