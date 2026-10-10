package com.northwind.returns.web;

import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.web.ReturnsFacade.ReturnOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/returns")
public class ReturnsController {

    private final ReturnsFacade returns;

    public ReturnsController(ReturnsFacade returns) {
        this.returns = returns;
    }

    @PostMapping("/messages")
    public ReturnOutcome fromMessage(@Valid @RequestBody MessageRequest request) {
        return returns.fromMessage(request.message());
    }

    @PostMapping("/form")
    public ReturnOutcome fromForm(@Valid @RequestBody ReturnRequest request) {
        return returns.fromForm(request);
    }

    /** Spring MVC completes the response when the future completes. */
    @PostMapping("/messages/async")
    public CompletableFuture<ReturnOutcome> fromMessageAsync(@Valid @RequestBody MessageRequest request) {
        return returns.fromMessageAsync(request.message());
    }

    public record MessageRequest(@NotBlank @Size(max = 4000) String message) {
    }
}
