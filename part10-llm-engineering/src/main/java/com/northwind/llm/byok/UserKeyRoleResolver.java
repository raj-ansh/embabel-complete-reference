package com.northwind.llm.byok;

import com.embabel.agent.spi.LlmService;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.ModelSelectionContext;
import com.embabel.common.ai.model.ProviderCredential;
import com.embabel.common.ai.model.RoleResolution;
import com.embabel.common.ai.model.RoleResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chapter 73: bring your own key. When the current user has stored a key, every role resolves to a
 * model built from that key, billed to the user rather than to NorthWind. Users without a key fall
 * through ({@code null}) to the deployment's configured roles.
 */
@Component
public class UserKeyRoleResolver implements RoleResolver {

    private final UserKeyStore keys;
    private final ObjectProvider<UserModelFactory> factory;
    private final Map<ProviderCredential, LlmService<?>> cache = new ConcurrentHashMap<>();

    public UserKeyRoleResolver(UserKeyStore keys, ObjectProvider<UserModelFactory> factory) {
        this.keys = keys;
        this.factory = factory;
    }

    @Override
    public RoleResolution resolve(String role, ModelSelectionContext context) {
        UserModelFactory models = factory.getIfAvailable();
        if (context.getUserId() == null || models == null) {
            return null;                                         // no user (or no BYOK support): use configured roles
        }
        return keys.activeKeyFor(context.getUserId())
                .map(credential -> cache.computeIfAbsent(credential, models::build))   // build once per key
                .map(service -> (RoleResolution) new RoleResolution.Service(service, LlmOptions.withModel(service.getName())))
                .orElse(null);                                    // user has no key: fall through
    }
}
