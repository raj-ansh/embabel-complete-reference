package com.northwind.greeting.web;

import com.northwind.greeting.service.Occasion;

public record GreetingResponse(String customerId, Occasion occasion, String greeting) {
}
