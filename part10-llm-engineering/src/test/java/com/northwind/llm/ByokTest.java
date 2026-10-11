package com.northwind.llm;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.ModelSelectionContext;
import com.embabel.common.ai.model.ModelSelectionContextHolder;
import com.embabel.common.ai.model.ProviderCredential;
import com.northwind.llm.byok.UserKeyStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 73: a user who brings a key is served by a model built from that key. */
class ByokTest extends LabTest {

    @Autowired Ai ai;
    @Autowired UserKeyStore keys;

    @AfterEach
    void forget() {
        keys.remove("asha");
    }

    private String askAs(String userId) {
        return ModelSelectionContextHolder.with(new ModelSelectionContext(userId, null),
                () -> ai.withLlmByRole("balanced").generateText(
                        "Write a short reply to the message between the <message> tags.\n<message>Where is my order?</message>"));
    }

    @Test
    void ownKeyOwnModel() {
        keys.save("asha", new ProviderCredential("Anthropic", "sk-ant-test-key-7f3a"));

        askAs("asha");
        askAs("mateo");                                   // no key stored: deployment's model

        recorder.calls().forEach(c -> System.out.println("  " + c));
        System.out.println("credential prints as: " + keys.activeKeyFor("asha").orElseThrow());
        assertThat(recorder.calls()).extracting(CallRecorder.Call::model)
                .containsExactly("byok-anthropic-7f3a", "lab-standard");
        assertThat(recorder.calls().getFirst().cost()).isZero();
    }
}
