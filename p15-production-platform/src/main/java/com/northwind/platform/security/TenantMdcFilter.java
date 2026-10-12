package com.northwind.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Chapter 107: every log line written while serving a request names its tenant. Added to the security
 * filter chain after authentication (not as a separate servlet filter, which would run before it).
 */
public class TenantMdcFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "tenant";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt
                && jwt.getToken().getClaimAsString("tenant") != null) {
            MDC.put(MDC_KEY, jwt.getToken().getClaimAsString("tenant"));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);                                       // pooled threads serve other tenants next
        }
    }
}
