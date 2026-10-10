package com.northwind.greeting.customer;

import java.time.LocalDate;

/** What the greeting service knows about a customer. Comes from our systems, not from the model. */
public record Customer(
        String id,
        String firstName,
        LoyaltyTier tier,
        String preferredLanguage,
        String lastPurchase,
        LocalDate memberSince) {

    public enum LoyaltyTier { STANDARD, SILVER, GOLD }
}
