package com.northwind.greeting.web;

import com.northwind.greeting.customer.Customer;
import com.northwind.greeting.customer.CustomerDirectory;
import com.northwind.greeting.customer.CustomerNotFoundException;
import com.northwind.greeting.service.GreetingService;
import com.northwind.greeting.service.Occasion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private final CustomerDirectory customers;
    private final GreetingService greetings;

    public GreetingController(CustomerDirectory customers, GreetingService greetings) {
        this.customers = customers;
        this.greetings = greetings;
    }

    @GetMapping("/api/customers/{id}/greeting")
    public GreetingResponse greeting(@PathVariable String id,
                                     @RequestParam(defaultValue = "WELCOME") Occasion occasion) {
        Customer customer = customers.find(id).orElseThrow(() -> new CustomerNotFoundException(id));
        return new GreetingResponse(id, occasion, greetings.greet(customer, occasion));
    }
}
