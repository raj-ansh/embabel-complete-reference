package com.northwind.mcpserver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Chapter 89: the MCP security starter protects /mcp, /sse and /message only. Without this chain, anyone
 * can send A2A messages to /a2a, and bearer tokens sent there are ignored. The agent card stays public,
 * as A2A expects; sending a message needs a token carrying the a2a:invoke authority.
 */
@Configuration
public class A2aSecurity {

    @Bean
    SecurityFilterChain a2aFilterChain(HttpSecurity http, JwtAuthenticationConverter embabelConverter) throws Exception {
        return http
                .securityMatcher("/a2a/**", "/a2a")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/a2a/.well-known/agent.json").permitAll()
                        .anyRequest().hasAuthority("a2a:invoke"))
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(embabelConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())               // bearer tokens, no cookies
                .build();
    }
}
