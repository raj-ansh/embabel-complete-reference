package com.northwind.enterprise;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.enterprise.billing.RefundDesk;
import com.northwind.enterprise.domain.Requests.CustomerRequest;
import com.northwind.enterprise.domain.Requests.Route;
import com.northwind.enterprise.lab.LabModels;
import com.northwind.enterprise.routing.RequestRouter;
import com.northwind.enterprise.service.SupportPlatform;
import com.northwind.enterprise.service.SupportPlatform.Reply;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 109: P14's specialists, router, refunds and guardrail, end to end with the lab models. */
@SpringBootTest
@ActiveProfiles("lab")
class PlatformTest {

    @DynamicPropertySource
    static void billing(DynamicPropertyRegistry registry) throws IOException {
        String url = BillingStub.start();
        registry.add("northwind.billing.url", () -> url);
    }

    @Autowired SupportPlatform platform;
    @Autowired RequestRouter router;
    @Autowired RefundDesk refunds;
    @Autowired @Qualifier("labMini") SpringAiLlmService mini;
    @Autowired @Qualifier("labStandard") SpringAiLlmService standard;

    private Reply ask(String customer, String text) {
        Reply reply = platform.handle(new CustomerRequest(customer, text));
        System.out.println(customer + ": \"" + text + "\" -> " + reply.route() + ": " + shorten(reply.text()));
        return reply;
    }

    private static String shorten(String text) {
        String compact = text.replaceAll("chunkId: \\S+ [0-9.]+ - ", "");
        return compact.length() > 150 ? compact.substring(0, 150) + "..." : compact;
    }

    @Test
    void routing() {
        for (String text : List.of("Where is my order NW-40001?", "Why was I charged twice on invoice INV-7001?",
                "Which tent is lightest for a solo trek?", "I want to make a formal complaint about your staff")) {
            var decision = router.route(new CustomerRequest("C-1001", text));
            System.out.println("route: " + decision.route() + " <- \"" + text + "\"");
        }
    }

    @Test
    void supportAndResearch() {
        assertThat(ask("C-1001", "Where is my order NW-40001?").text()).contains("SHIPPED");
        assertThat(ask("C-1002", "Where is my order NW-40001?").text()).contains("No order");
        assertThat(ask("C-1001", "How long do I have to return a tent?").text()).contains("30 days");
        assertThat(ask("C-1001", "Which tent is lightest for a solo trek?").text()).contains("Featherline");
        assertThat(ask("C-1001", "I want to make a formal complaint about your staff").route()).isEqualTo(Route.HUMAN);
    }

    @Test
    void billingAndRefunds() {
        assertThat(ask("C-1002", "What do I still owe on invoice INV-7003?").text()).contains("UNPAID");
        assertThat(ask("C-1001", "Please refund 20 pounds on invoice INV-7001, the stove was faulty").text())
                .contains("Refunded 20");
        var large = ask("C-1001", "Please refund 150 pounds on invoice INV-7002, the tent leaked");
        assertThat(large.text()).contains("sent to our team for approval");
        var pending = refunds.pending();
        System.out.println("pending approvals: " + pending);
        String reference = refunds.approve(pending.getFirst().id(), "S-77");
        System.out.println("approved by S-77, billing reference: " + reference + ", refunds sent to billing: "
                + BillingStub.refundsReceived.size());
        assertThat(refunds.pending()).isEmpty();
    }

    @Test
    void cardNumbersNeverReachAModel() {
        int before = LabModels.scripted(mini).received().size() + LabModels.scripted(standard).received().size();
        var reply = ask("C-1001", "My card 4111 1111 1111 1111 was charged twice for invoice INV-7001");
        int after = LabModels.scripted(mini).received().size() + LabModels.scripted(standard).received().size();
        System.out.println("model calls for the message with a card number: " + (after - before));
        assertThat(after).isEqualTo(before);
        assertThat(reply.text()).contains("don't send card numbers");
    }
}
