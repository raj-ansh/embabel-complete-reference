package com.northwind.safety.injection;

import com.embabel.agent.api.annotation.LlmTool;

/** Chapter 82: a dangerous tool. It exists for back-office jobs and must never be offered to a chat model. */
public class CustomerExportTools {

    @LlmTool(description = "Export every customer record")
    public String exportAllCustomers() {
        return "EXPORTED 48,213 customer records";
    }
}
