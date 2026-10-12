package com.northwind.tenancy.tenant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Chapter 103: resolves the tenant from the authenticated token's "tenant" claim. The token is signed by the
 * identity provider, so neither the client nor the model can choose a tenant. Unknown and suspended tenants
 * are refused before any work starts.
 */
@Component
public class TenantResolver {

    private final Tenants tenants;

    public TenantResolver(Tenants tenants) {
        this.tenants = tenants;
    }

    public TenantContext resolve(Jwt token) {
        String tenantId = token.getClaimAsString("tenant");
        Tenants.Settings settings = tenantId == null ? null : tenants.get(tenantId);
        if (settings == null || !settings.active()) {
            throw new AccessDeniedException("No active tenant for this token");
        }
        return new TenantContext(tenantId, settings.name(), settings.plan(), token.getSubject(), settings.voice());
    }
}
