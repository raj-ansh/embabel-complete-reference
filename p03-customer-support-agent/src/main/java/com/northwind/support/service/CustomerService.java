package com.northwind.support.service;

import com.northwind.support.domain.SupportDomain.CustomerProfile;
import com.northwind.support.domain.SupportDomain.CustomerProfile.Tier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomerService {

    private final Map<String, CustomerProfile> customers = Map.of(
            "C-1001", new CustomerProfile("C-1001", "Asha", Tier.GOLD, 0),
            "C-1002", new CustomerProfile("C-1002", "Mateo", Tier.STANDARD, 2));

    public Optional<CustomerProfile> find(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}
