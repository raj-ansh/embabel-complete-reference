package com.northwind.platform.web;

import com.northwind.platform.billing.RefundDesk;
import com.northwind.platform.billing.RefundDesk.PendingRefund;
import com.northwind.platform.domain.Requests.CustomerRequest;
import com.northwind.platform.service.SupportPlatform;
import com.northwind.platform.service.SupportPlatform.Reply;
import com.northwind.platform.tenant.TenantResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * P15: customers ask; staff approve refunds for their own tenant. Identity, tenant and roles all come from the
 * verified token (Chapters 84 and 103); no request field can choose them.
 */
@RestController
public class SupportController {

    public record Message(@NotBlank @Size(max = 2000) String text) {
    }

    private final TenantResolver tenants;
    private final SupportPlatform platform;
    private final RefundDesk refunds;

    public SupportController(TenantResolver tenants, SupportPlatform platform, RefundDesk refunds) {
        this.tenants = tenants;
        this.platform = platform;
        this.refunds = refunds;
    }

    @PostMapping("/api/requests")
    public Reply ask(@AuthenticationPrincipal Jwt token, @Valid @RequestBody Message message) {
        var tenant = tenants.resolve(token);
        return platform.handle(tenant, new CustomerRequest(token.getSubject(), message.text()));
    }

    @GetMapping("/api/staff/refunds")
    public List<PendingRefund> pending(@AuthenticationPrincipal Jwt token) {
        return refunds.pending(tenants.resolve(token).tenantId());
    }

    @PostMapping("/api/staff/refunds/{id}/approve")
    public ResponseEntity<Map<String, String>> approve(@AuthenticationPrincipal Jwt token, @PathVariable long id) {
        var tenant = tenants.resolve(token);
        try {
            return ResponseEntity.ok(Map.of("reference", refunds.approve(id, token.getSubject(), tenant.tenantId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();                  // unknown, decided, or another tenant's
        }
    }
}
