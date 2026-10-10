package com.northwind.purchasing.web;

import com.northwind.purchasing.domain.Purchasing.Requester;
import com.northwind.purchasing.web.ApprovalService.ConflictException;
import com.northwind.purchasing.web.ApprovalService.ForbiddenException;
import com.northwind.purchasing.web.ApprovalService.ProcessView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * P9's API. Headers stand in for the authenticated principal (Chapter 84 replaces them with Spring Security).
 * POST /api/purchases                     submit a request (employee)
 * GET  /api/purchases/{id}                see status and what it is waiting for
 * POST /api/purchases/{id}/decision       approve or reject (budget holder, not the requester)
 */
@RestController
public class PurchaseController {

    public record PurchaseText(@NotBlank @Size(max = 2000) String text) {
    }

    public record Decision(boolean approved, @Size(max = 500) String comment) {
    }

    private final ApprovalService approvals;

    public PurchaseController(ApprovalService approvals) {
        this.approvals = approvals;
    }

    @PostMapping("/api/purchases")
    public ProcessView submit(@RequestHeader("X-Employee-Id") String employeeId,
                              @RequestHeader("X-Department") String department,
                              @Valid @RequestBody PurchaseText body) throws Exception {
        return approvals.submit(body.text(), new Requester(employeeId, department));
    }

    @GetMapping("/api/purchases/{id}")
    public ProcessView status(@PathVariable String id) {
        return approvals.status(id);
    }

    @PostMapping("/api/purchases/{id}/decision")
    public ProcessView decide(@PathVariable String id,
                              @RequestHeader("X-Employee-Id") String approverId,
                              @RequestHeader(value = "X-Roles", defaultValue = "") String roles,
                              @Valid @RequestBody Decision body) throws Exception {
        boolean budgetHolder = java.util.Arrays.asList(roles.split(",")).contains("budget-holder");
        return approvals.decide(id, approverId, budgetHolder, body.approved(), body.comment());
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
