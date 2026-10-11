package com.northwind.safety.security;

import com.embabel.agent.api.identity.SimpleUser;
import com.embabel.agent.core.Identities;
import com.embabel.agent.core.ProcessOptions;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

/**
 * Chapter 84: who is calling, taken only from the verified token. Nothing in a request body or a prompt
 * can change it. The same identity reaches Embabel twice: as the process user, and in tool call context.
 */
public record CallerIdentity(String userId, String tenant) {

    public static CallerIdentity from(Jwt token) {
        return new CallerIdentity(token.getSubject(), token.getClaimAsString("tenant"));
    }

    public ProcessOptions processOptions() {
        return new ProcessOptions()
                .withIdentities(new Identities(new SimpleUser(userId, userId, userId, null)))
                .withToolCallContext(toolContext());
    }

    public Map<String, Object> toolContext() {
        return Map.of("customerId", userId, "tenant", tenant == null ? "" : tenant);
    }
}
