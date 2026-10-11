package com.northwind.safety.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Chapter 84: every API request carries a JWT from NorthWind's identity provider. Roles come from the
 * token's "roles" claim. Embabel's built-in process endpoints are for operators only. Each user is
 * rate-limited after authentication, so limits apply per person rather than per IP address.
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, SecurityProperties properties) throws Exception {
        http.securityMatcher("/api/**", "/events/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/process/**", "/events/**", "/api/costs/**").hasRole("OPS")
                        .requestMatchers("/api/refunds/*/approve").hasRole("SUPERVISOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())                  // stateless bearer-token API: no cookies to forge
                .addFilterAfter(new RateLimitFilter(properties.requestsPerMinute()), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /** "roles": ["CUSTOMER"] in the token becomes the authority ROLE_CUSTOMER. */
    private static JwtAuthenticationConverter rolesFromToken() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
