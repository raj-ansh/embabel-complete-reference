package com.northwind.llm.service;

import com.embabel.agent.api.common.Ai;
import com.northwind.llm.audit.EmailRedactingTransformer;
import com.northwind.llm.audit.ModelCallInspector;
import com.northwind.llm.audit.ToolAuditInspector;
import com.northwind.llm.tools.OrderTools;
import org.springframework.stereotype.Service;

/** Chapter 79: an order question answered with tools, audited and redacted. */
@Service
public class OrderAnswers {

    private final Ai ai;
    private final ToolAuditInspector toolAudit;
    private final ModelCallInspector modelAudit;
    private final EmailRedactingTransformer redactor;

    public OrderAnswers(Ai ai, ToolAuditInspector toolAudit, ModelCallInspector modelAudit,
                        EmailRedactingTransformer redactor) {
        this.ai = ai;
        this.toolAudit = toolAudit;
        this.modelAudit = modelAudit;
        this.redactor = redactor;
    }

    public String answer(String question) {
        return ai.withLlmByRole("balanced")
                .withToolObject(new OrderTools())
                .withToolCallInspectors(toolAudit)          // observe each tool call
                .withToolLoopInspectors(modelAudit)         // observe each model round trip
                .withToolLoopTransformers(redactor)         // change what the model sees
                .generateText(question);
    }
}
