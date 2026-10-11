package com.northwind.mcpserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Chapter 89: Embabel's A2A endpoint puts the exception's message and full stack trace into JSON-RPC
 * errors. The server log already has them; a remote agent should not. This filter rebuilds each error
 * from its code alone. Streaming requests (text/event-stream) are passed through untouched.
 */
@Component
public class A2aErrorRedaction extends OncePerRequestFilter {

    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String accept = String.valueOf(request.getHeader("Accept"));
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().equals("/a2a")
                || accept.contains("text/event-stream");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var buffered = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, buffered);
        byte[] body = buffered.getContentAsByteArray();
        try {
            if (json.readTree(body) instanceof ObjectNode reply && reply.get("error") instanceof ObjectNode error) {
                reply.set("error", json.createObjectNode()
                        .put("code", error.path("code").asInt(-32603))
                        .put("message", "The request could not be completed"));
                body = json.writeValueAsBytes(reply);
            }
        } catch (JacksonException notJson) {
            // not a JSON-RPC reply: pass it on unchanged
        }
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }
}
