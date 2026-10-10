package com.northwind.content;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.content.domain.Copy.Status;
import com.northwind.content.workflow.CopyWorkflowService;
import com.northwind.content.workflow.CopyWorkflowService.WorkflowView;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 67: a waiting workflow is finished by a different application instance. Each nested class gets its
 * own Spring context (different properties), so its own in-memory process repository, while both share one
 * database. The second instance has never seen the process: it can only finish it by restoring the snapshot.
 */
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class RestartRecoveryTest {

    static final String SHARED_DB =
            "spring.datasource.url=jdbc:h2:file:./target/restart-db/content;MODE=PostgreSQL;AUTO_SERVER=TRUE";

    static String processId;

    @Nested
    @Order(1)
    @TestPropertySource(properties = {SHARED_DB, "northwind.instance=1"})
    class FirstInstance extends EmbabelMockitoIntegrationTest {

        @Autowired
        CopyWorkflowService workflows;

        @Autowired
        JdbcTemplate jdbc;

        @Test
        void startsTheWorkflowAndLeavesItWaiting() throws Exception {
            whenGenerateText(p -> p.contains("Write a product description"))
                    .thenReturn("Two doors, a 3,000 mm flysheet and 2.1 kg: the Ridgeline 2P pitches in five minutes.");

            WorkflowView started = workflows.start(CopyWorkflowTest.TENT);
            processId = started.processId();

            System.out.println("instance 1: " + processId + " " + started.status() + ", snapshot "
                    + jdbc.queryForMap("select status, version from agent_process_snapshots where process_id = ?", processId));
            assertThat(started.status()).isEqualTo("WAITING");
        }
    }

    @Nested
    @Order(2)
    @TestPropertySource(properties = {SHARED_DB, "northwind.instance=2"})
    class SecondInstance extends EmbabelMockitoIntegrationTest {

        @Autowired
        CopyWorkflowService workflows;

        @Test
        void restoresTheWorkflowFromItsSnapshotAndFinishesIt() throws Exception {
            WorkflowView restored = workflows.status(processId);
            System.out.println("instance 2: " + restored.processId() + " " + restored.status() + " -- "
                    + restored.waitingFor().lines().findFirst().orElse(""));

            WorkflowView done = workflows.review(processId, "ED-7", true, "Approved after the weekend.");
            System.out.println("instance 2: " + done.status() + " " + done.outcome());

            assertThat(restored.status()).isEqualTo("WAITING");
            assertThat(done.outcome().status()).isEqualTo(Status.PUBLISHED);
        }
    }
}
