package com.northwind.llm.config;

import com.embabel.common.ai.model.LlmOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Which model, and which settings, each step uses. Bound from {@code northwind.models} so that
 * operations can change a model without a release. {@link LlmOptions} binds like any JavaBean.
 */
@ConfigurationProperties("northwind.models")
public record ModelChoices(LlmOptions triage, LlmOptions reply, LlmOptions sensitiveReply,
                           List<String> replyFallbacks) {
}
