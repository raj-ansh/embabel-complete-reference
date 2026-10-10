package com.northwind.springai;

import com.northwind.springai.triage.TicketTriage;
import com.northwind.springai.triage.TriageService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

class TriageServiceTest {

    @Test
    void structuredOutputBecomesARecord() {
        var model = new StubChatModel("""
                {"category": "SHIPPING", "urgency": 4, "summary": "Tent pole arrived snapped."}""");
        var service = new TriageService(ChatClient.builder(model).build());

        TicketTriage triage = service.triage("My tent pole arrived snapped. Order NW-10442.");

        assertThat(triage.category()).isEqualTo(TicketTriage.Category.SHIPPING);
        assertThat(triage.urgency()).isEqualTo(4);
        // Spring AI appended format instructions (a JSON schema) to the prompt for us
        assertThat(model.lastPrompt().getContents()).contains("NW-10442").containsIgnoringCase("json");
    }

    @Test
    void plainTextCall() {
        var model = new StubChatModel("Customer reports a snapped tent pole on order NW-10442.");
        var service = new TriageService(ChatClient.builder(model).build());

        assertThat(service.summarise("…long message…")).contains("NW-10442");
    }
}
