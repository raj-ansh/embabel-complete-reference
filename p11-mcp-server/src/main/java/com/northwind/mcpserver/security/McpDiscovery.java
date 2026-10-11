package com.northwind.mcpserver.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Chapter 88: when a request to /mcp has no token, the 401 response points MCP clients at
 * /.well-known/oauth-protected-resource to discover where to get one. The MCP security starter's filter
 * chain does not cover that path, so without this chain it returns 404.
 */
@Configuration
public class McpDiscovery {

    @Bean
    SecurityFilterChain mcpDiscoveryChain(HttpSecurity http,
                                          @Value("${northwind.security.authorization-server}") String issuer)
            throws Exception {
        return http
                .securityMatcher("/.well-known/oauth-protected-resource/**", "/.well-known/oauth-protected-resource")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> { })
                        .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(m -> m
                                .resourceName("NorthWind MCP server")
                                .authorizationServer(issuer)
                                .scope("orders:read")
                                .scope("products:read")
                                .tlsClientCertificateBoundAccessTokens(false))))
                .build();
    }
}
