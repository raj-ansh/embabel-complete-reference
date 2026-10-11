package com.northwind.mcpclient.security;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Chapter 88: adds the current user's token to every HTTP request the MCP client sends. Spring AI applies
 * any McpSyncHttpClientRequestCustomizer bean to its HTTP-based MCP connections.
 */
@Component
public class TokenRelay implements McpSyncHttpClientRequestCustomizer {

    @Override
    public void customize(HttpRequest.Builder request, String method, URI endpoint, String body, McpTransportContext context) {
        String token = CurrentUserToken.get();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
    }
}
