package com.northwind.greeting.customer;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** An in-memory stand-in for NorthWind's CRM. Later projects replace it with a real repository. */
@Service
public class CustomerDirectory {

    private final Map<String, Customer> customers = Map.of(
            "C-1001", new Customer("C-1001", "Asha", Customer.LoyaltyTier.GOLD, "en",
                    "Ridgeline 2-Person Tent", LocalDate.of(2021, 4, 12)),
            "C-1002", new Customer("C-1002", "Mateo", Customer.LoyaltyTier.STANDARD, "es",
                    "Trailblazer Waterproof Boot", LocalDate.of(2026, 9, 30)));

    public Optional<Customer> find(String id) {
        return Optional.ofNullable(customers.get(id));
    }
}
