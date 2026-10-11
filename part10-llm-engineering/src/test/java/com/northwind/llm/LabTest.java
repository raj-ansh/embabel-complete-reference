package com.northwind.llm;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.llm.lab.LabModels;
import com.northwind.llm.lab.ScriptedChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Base class: the whole application on the offline lab models. */
@SpringBootTest
@ActiveProfiles("lab")
public abstract class LabTest {

    @Autowired protected SpringAiLlmService labMini;
    @Autowired protected SpringAiLlmService labStandard;
    @Autowired protected SpringAiLlmService labReasoner;
    @Autowired protected CallRecorder recorder;

    protected ScriptedChatModel mini() {
        return LabModels.scripted(labMini);
    }

    protected ScriptedChatModel standard() {
        return LabModels.scripted(labStandard);
    }

    protected ScriptedChatModel reasoner() {
        return LabModels.scripted(labReasoner);
    }

    @BeforeEach
    void resetLab() {
        recorder.clear();
        mini().clear();
        standard().clear();
        reasoner().clear();
    }
}
