package com.northwind.llm.byok;

import com.embabel.common.ai.model.ProviderCredential;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chapter 73: where a user's own API key lives. In production this is an encrypted table or a secrets
 * manager (Chapter 84); never a log, never a prompt, never the blackboard.
 */
@Component
public class UserKeyStore {

    private final Map<String, ProviderCredential> keys = new ConcurrentHashMap<>();

    public void save(String userId, ProviderCredential credential) {
        keys.put(userId, credential);
    }

    public void remove(String userId) {
        keys.remove(userId);
    }

    public Optional<ProviderCredential> activeKeyFor(String userId) {
        return Optional.ofNullable(keys.get(userId));
    }
}
