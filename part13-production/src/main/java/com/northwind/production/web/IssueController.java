package com.northwind.production.web;

import com.embabel.agent.core.ProcessOptions;
import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.service.IssueService;
import com.northwind.production.service.IssueService.Outcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;

/**
 * Chapter 97: the HTTP API around the agent. The caller's identity comes from a header here to keep the
 * examples small; in production it is the authenticated principal (Chapter 84), never a request field.
 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    static final String CUSTOMER = "X-Customer-Id";

    public record IssueRequest(
            @NotBlank @Pattern(regexp = "NW-\\d{5}") String orderNumber,
            @NotBlank @Size(max = 2000) String message) {
    }

    public record IssueResponse(String id, String status, String reply, String remedy, String message) {

        static IssueResponse of(Outcome outcome) {
            return new IssueResponse(outcome.processId(), outcome.status().name(),
                    outcome.reply() == null ? null : outcome.reply().text(),
                    outcome.reply() == null ? null : outcome.reply().remedy().name(),
                    outcome.message());
        }
    }

    private final IssueService issues;
    private final IssueProgress progress;

    public IssueController(IssueService issues, IssueProgress progress) {
        this.issues = issues;
        this.progress = progress;
    }

    /** Waits for the answer: simple for clients, but holds a server thread for the whole run. */
    @PostMapping
    public ResponseEntity<IssueResponse> resolve(@RequestHeader(CUSTOMER) String customerId,
                                                 @Valid @RequestBody IssueRequest request) {
        Outcome outcome = issues.resolve(new IssueReport(customerId, request.orderNumber(), request.message()));
        HttpStatus status = switch (outcome.status()) {
            case RESOLVED -> HttpStatus.OK;
            case REFUSED -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(IssueResponse.of(outcome));
    }

    /** Starts the work and answers at once; the client polls, streams progress or cancels. */
    @PostMapping("/async")
    public ResponseEntity<IssueResponse> start(@RequestHeader(CUSTOMER) String customerId,
                                               @Valid @RequestBody IssueRequest request) {
        String id = issues.start(new IssueReport(customerId, request.orderNumber(), request.message()), new ProcessOptions());
        return ResponseEntity.accepted().location(URI.create("/api/issues/" + id))
                .body(new IssueResponse(id, "IN_PROGRESS", null, null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> outcome(@RequestHeader(CUSTOMER) String customerId, @PathVariable String id) {
        if (!issues.isOwnedBy(id, customerId)) {
            return ResponseEntity.notFound().build();              // same answer for "unknown" and "not yours"
        }
        return issues.outcome(id).map(o -> ResponseEntity.ok(IssueResponse.of(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@RequestHeader(CUSTOMER) String customerId, @PathVariable String id) {
        if (!issues.isOwnedBy(id, customerId)) {
            return ResponseEntity.notFound().build();
        }
        return issues.cancel(id) ? ResponseEntity.accepted().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    /** Chapter 98: progress as server-sent events. */
    @GetMapping(path = "/{id}/events", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> events(@RequestHeader(CUSTOMER) String customerId, @PathVariable String id) {
        if (!issues.isOwnedBy(id, customerId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress.subscribe(id));
    }
}
