package com.northwind.llm.byok;

import com.embabel.agent.spi.LlmService;
import com.embabel.common.ai.model.ProviderCredential;

/**
 * Chapter 73: builds a model service from a user's key. A production implementation delegates to
 * Embabel's BYOK factories ({@code embabel-agent-starter-byok}), which validate the key with one
 * probe call; the lab implementation ({@code LabModels}) builds a scripted model instead.
 */
@FunctionalInterface
public interface UserModelFactory {

    LlmService<?> build(ProviderCredential credential);
}
