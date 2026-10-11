package com.northwind.safety;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.safety.lab.LabModels;
import com.northwind.safety.lab.ScriptedChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Base class: the whole application on the naive offline lab model. */
@SpringBootTest
@ActiveProfiles("lab")
public abstract class LabTest {

    @Autowired protected SpringAiLlmService labAssistant;

    protected ScriptedChatModel model() {
        return LabModels.scripted(labAssistant);
    }

    @BeforeEach
    void resetLab() {
        model().clear();
    }

    /** Describes an exception and its causes on one line, for printed test output. */
    protected static String describe(Throwable e) {
        StringBuilder out = new StringBuilder(e.getClass().getSimpleName() + ": " + e.getMessage());
        for (Throwable t = e.getCause(); t != null && t != t.getCause(); t = t.getCause()) {
            out.append("\n  caused by ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
        }
        return out.toString();
    }
}
