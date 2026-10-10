package com.northwind.content;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.content.domain.Copy.CopyBrief;
import com.northwind.content.domain.Copy.Status;
import com.northwind.content.workflow.CopyWorkflowService;
import com.northwind.content.workflow.CopyWorkflowService.WorkflowView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapters 64-67: states, looping, human review and JDBC snapshots, with a stubbed writer model. */
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:content;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class CopyWorkflowTest extends EmbabelMockitoIntegrationTest {

    static final CopyBrief TENT = new CopyBrief("TENT-2P", "Ridgeline 2-Person Tent",
            List.of("2.1 kg", "3,000 mm waterproof flysheet", "two doors", "pitches in 5 minutes"), 60);

    @Autowired
    CopyWorkflowService workflows;

    @Autowired
    JdbcTemplate jdbc;

    private void writerReturns(String firstDraft, String revision) {
        whenGenerateText(p -> p.contains("Write a product description") && !p.contains("<previous-draft>"))
                .thenReturn(firstDraft);
        whenGenerateText(p -> p.contains("<previous-draft>")).thenReturn(revision);
    }

    private Map<String, Object> snapshot(String processId) {
        return jdbc.queryForMap("select status, version, agent_name from agent_process_snapshots where process_id = ?",
                processId);
    }

    @Test
    void reviseOnceThenPublish() throws Exception {
        writerReturns("The Ridgeline is a tent.",
                "Two doors, a 3,000 mm flysheet and 2.1 kg: the Ridgeline 2P pitches in five minutes.");

        WorkflowView round1 = workflows.start(TENT);
        System.out.println("started:   " + round1.status() + " -- " + round1.waitingFor().lines().findFirst().orElse(""));
        System.out.println("snapshot:  " + snapshot(round1.processId()));

        WorkflowView round2 = workflows.review(round1.processId(), "ED-7", false, "Too thin. Use the facts.");
        System.out.println("revised:   " + round2.status() + " -- " + round2.waitingFor().lines().findFirst().orElse(""));

        WorkflowView done = workflows.review(round1.processId(), "ED-7", true, "Good.");
        System.out.println("finished:  " + done.status() + " " + done.outcome());
        System.out.println("snapshot:  " + snapshot(round1.processId()));

        assertThat(round1.status()).isEqualTo("WAITING");
        assertThat(round2.waitingFor()).startsWith("Review round 2");
        assertThat(done.outcome().status()).isEqualTo(Status.PUBLISHED);
        assertThat(done.outcome().rounds()).isEqualTo(2);
        assertThat(snapshot(round1.processId())).containsEntry("STATUS", "COMPLETED");
    }

    @Test
    void escalateAfterThreeRejections() throws Exception {
        writerReturns("Draft one.", "Another draft.");

        WorkflowView view = workflows.start(TENT);
        for (int round = 1; round <= 3; round++) {
            view = workflows.review(view.processId(), "ED-9", false, "Still not right (round " + round + ").");
        }

        System.out.println("escalated: " + view.status() + " " + view.outcome());
        assertThat(view.outcome().status()).isEqualTo(Status.ESCALATED);
        assertThat(view.outcome().rounds()).isEqualTo(3);
    }
}
