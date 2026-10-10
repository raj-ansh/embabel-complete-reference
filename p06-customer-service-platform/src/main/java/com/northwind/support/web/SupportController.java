package com.northwind.support.web;

import com.northwind.support.web.SupportService.ConflictException;
import com.northwind.support.web.SupportService.ForbiddenException;
import com.northwind.support.web.SupportService.SupportView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * P6's API. Headers stand in for authentication (Chapter 84).
 * POST /api/support                               a customer's message
 * POST /api/support/{id}/refund-decision          a support lead approves or rejects a large refund
 */
@RestController
public class SupportController {

    public record Message(@NotBlank @Size(max = 2000) String message) {
    }

    public record RefundDecision(boolean approved, @NotBlank @Size(max = 500) String note) {
    }

    private final SupportService support;

    public SupportController(SupportService support) {
        this.support = support;
    }

    @PostMapping("/api/support")
    public SupportView ask(@RequestHeader("X-Customer-Id") String customerId,
                           @Valid @RequestBody Message body) throws Exception {
        return support.ask(customerId, body.message());
    }

    @PostMapping("/api/support/{id}/refund-decision")
    public SupportView decide(@PathVariable String id,
                              @RequestHeader("X-Employee-Id") String employeeId,
                              @RequestHeader(value = "X-Roles", defaultValue = "") String roles,
                              @Valid @RequestBody RefundDecision body) throws Exception {
        boolean lead = Arrays.asList(roles.split(",")).contains("support-lead");
        return support.decideRefund(id, employeeId, lead, body.approved(), body.note());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
