package com.northwind.tenancy.web;

import com.northwind.tenancy.budget.TenantSpend;
import com.northwind.tenancy.service.TenantSupportService;
import com.northwind.tenancy.service.TenantSupportService.Reply;
import com.northwind.tenancy.tenant.TenantResolver;
import com.northwind.tenancy.tenant.Tenants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** P13: the API. Neither endpoint accepts a tenant: it always comes from the caller's token. */
@RestController
public class SupportController {

    public record Question(@NotBlank @Size(max = 1000) String question) {
    }

    public record Usage(String tenant, BigDecimal spentTodayUsd, BigDecimal dailyBudgetUsd) {
    }

    private final TenantResolver resolver;
    private final TenantSupportService support;
    private final TenantSpend spend;
    private final Tenants tenants;

    public SupportController(TenantResolver resolver, TenantSupportService support, TenantSpend spend, Tenants tenants) {
        this.resolver = resolver;
        this.support = support;
        this.spend = spend;
        this.tenants = tenants;
    }

    @PostMapping("/api/support")
    public Reply ask(@AuthenticationPrincipal Jwt token, @Valid @RequestBody Question question) {
        return support.answer(resolver.resolve(token), question.question());
    }

    /** Chapter 107: a tenant administrator sees their own tenant's usage, never another's. */
    @GetMapping("/api/admin/usage")
    public Usage usage(@AuthenticationPrincipal Jwt token) {
        var tenant = resolver.resolve(token);
        return new Usage(tenant.tenantId(), spend.spentToday(tenant.tenantId()),
                tenants.get(tenant.tenantId()).dailyBudgetUsd());
    }
}
