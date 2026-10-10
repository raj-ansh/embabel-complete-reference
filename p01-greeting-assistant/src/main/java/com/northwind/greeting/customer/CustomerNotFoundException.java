package com.northwind.greeting.customer;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String id) {
        super("No customer with id " + id);
    }
}
