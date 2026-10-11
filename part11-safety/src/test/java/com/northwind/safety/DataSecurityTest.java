package com.northwind.safety;

import com.embabel.agent.api.tool.Tool;
import com.northwind.safety.privacy.Pseudonymiser;
import com.northwind.safety.rag.KnowledgeBase;
import com.northwind.safety.service.PrivateReplies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 83: pseudonymised prompts, and a knowledge base that resists poisoning. */
class DataSecurityTest extends LabTest {

    @Autowired PrivateReplies replies;
    @Autowired KnowledgeBase knowledge;

    @Test
    void modelNeverSeesContactDetails() {
        var exchange = replies.reply("My stove leaks. Please email me at asha.patel@example.com or call 07700 900123. Postcode LS1 4AP.");
        System.out.println("sent to model:   " + exchange.sentToModel());
        System.out.println("model replied:   " + exchange.fromModel());
        System.out.println("customer sees:   " + exchange.toCustomer());
        assertThat(exchange.sentToModel()).doesNotContain("asha.patel", "07700", "LS1");
        assertThat(exchange.toCustomer()).contains("asha.patel@example.com");
    }

    @Test
    void pseudonymsAreConsistentWithinAConversation() {
        var p = new Pseudonymiser();
        System.out.println(p.mask("Write to a@example.com, copy b@example.com, and again a@example.com."));
    }

    private static String search(KnowledgeBase kb, boolean officialOnly, String query) {
        Tool vector = kb.search(officialOnly).tools().stream()
                .filter(t -> t.getDefinition().getName().endsWith("vectorSearch")).findFirst().orElseThrow();
        return vector.call("{\"query\": \"" + query + "\", \"topK\": 3}").toString();   // exactly what a model would get
    }

    @Test
    void poisonedKnowledgeIsQuarantinedOrFiltered() throws Exception {
        Path root = new ClassPathResource("knowledge").getFile().toPath();
        var report = knowledge.ingest(root);
        report.ingested().forEach(u -> System.out.println("ingested:    " + u));
        report.quarantined().forEach(u -> System.out.println("quarantined: " + u));

        String all = search(knowledge, false, "can I cook inside my tent with the stove");
        String official = search(knowledge, true, "can I cook inside my tent with the stove");
        System.out.println("all sources cite the forum tip:      " + all.contains("Forum Tip"));
        System.out.println("official sources cite the forum tip: " + official.contains("Forum Tip"));
        System.out.println("official sources cite the safety page: " + official.contains("Never use the stove inside a tent"));
        assertThat(official).doesNotContain("Forum Tip");
        assertThat(report.quarantined()).anyMatch(q -> q.contains("supplier-spec-sheet"));
    }
}
