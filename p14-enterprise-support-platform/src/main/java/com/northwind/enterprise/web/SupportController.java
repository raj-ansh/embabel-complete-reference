package com.northwind.enterprise.web;

import com.northwind.enterprise.billing.RefundDesk;
import com.northwind.enterprise.billing.RefundDesk.PendingRefund;
import com.northwind.enterprise.domain.Requests.CustomerRequest;
import com.northwind.enterprise.service.SupportPlatform;
import com.northwind.enterprise.service.SupportPlatform.Reply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * P14: customers ask; staff approve refunds. Identity comes from headers here to keep P14 small; P15 replaces
 * them with JWT authentication and tenant resolution (Chapter 112).
 */
@RestController
public class SupportController {

    public record Message(@NotBlank @Size(max = 2000) String text) {
    }

    private final SupportPlatform platform;
    private final RefundDesk refunds;

    public SupportController(SupportPlatform platform, RefundDesk refunds) {
        this.platform = platform;
        this.refunds = refunds;
    }

    @PostMapping("/api/requests")
    public Reply ask(@RequestHeader("X-Customer-Id") String customerId, @Valid @RequestBody Message message) {
        return platform.handle(new CustomerRequest(customerId, message.text()));
    }

    @GetMapping("/api/staff/refunds")
    public List<PendingRefund> pending() {
        return refunds.pending();
    }

    @PostMapping("/api/staff/refunds/{id}/approve")
    public Map<String, String> approve(@RequestHeader("X-Staff-Id") String staff, @PathVariable long id) {
        return Map.of("reference", refunds.approve(id, staff));
    }
}
