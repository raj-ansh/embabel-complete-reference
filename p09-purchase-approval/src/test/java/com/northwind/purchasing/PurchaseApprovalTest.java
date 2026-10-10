package com.northwind.purchasing;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.purchasing.domain.Purchasing.PurchaseRequest;
import com.northwind.purchasing.domain.Purchasing.Requester;
import com.northwind.purchasing.domain.Purchasing.RiskNote;
import com.northwind.purchasing.web.ApprovalService;
import com.northwind.purchasing.web.ApprovalService.ForbiddenException;
import com.northwind.purchasing.web.ApprovalService.ProcessView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P9: every route through the approval process, including the pause and the resume. */
class PurchaseApprovalTest extends EmbabelMockitoIntegrationTest {

    private static final Requester ASHA = new Requester("E-17", "Guiding");

    @Autowired
    ApprovalService approvals;

    private void modelReads(String marker, PurchaseRequest request) {
        whenCreateObject(p -> p.contains("<request>") && p.contains(marker), PurchaseRequest.class).thenReturn(request);
        whenCreateObject(p -> p.contains("<purchase"), RiskNote.class)
                .thenReturn(new RiskNote("GPS units for the guiding team's new routes.", List.of("No supplier quote attached")));
    }

    @Test
    void smallPurchasesAreApprovedByPolicy() throws Exception {
        modelReads("maps", new PurchaseRequest("MapCo", "trail maps", 40, new BigDecimal("12.50"), "Shop stock"));

        ProcessView view = approvals.submit("40 trail maps from MapCo at 12.50", ASHA);

        System.out.println("small:  " + view);
        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.result().decidedBy()).isEqualTo("policy");
    }

    @Test
    void hugePurchasesAreRejectedByPolicy() throws Exception {
        modelReads("vans", new PurchaseRequest("AutoFleet", "minibus", 2, new BigDecimal("31000"), "Shuttle service"));

        ProcessView view = approvals.submit("2 minibus vans from AutoFleet at 31000", ASHA);

        System.out.println("huge:   " + view);
        assertThat(view.result().status()).isEqualTo("REJECTED");
    }

    @Test
    void largerPurchasesWaitForABudgetHolderAndThenResume() throws Exception {
        modelReads("GPS", new PurchaseRequest("TrailTech", "GPS units", 12, new BigDecimal("249.00"), "Guided hikes"));

        ProcessView waiting = approvals.submit("12 GPS units from TrailTech at 249 each for guided hikes", ASHA);
        System.out.println("waiting: " + waiting);
        assertThat(waiting.status()).isEqualTo("WAITING");
        assertThat(waiting.waitingFor()).contains("2988.00", "No supplier quote attached");

        ProcessView decided = approvals.decide(waiting.processId(), "M-3", true, true, "OK, within the Q3 budget");
        System.out.println("decided: " + decided);
        assertThat(decided.status()).isEqualTo("COMPLETED");
        assertThat(decided.result().status()).isEqualTo("ORDERED");
        assertThat(decided.result().decidedBy()).isEqualTo("M-3");
    }

    @Test
    void nobodyApprovesTheirOwnRequestAndOnlyBudgetHoldersDecide() throws Exception {
        modelReads("GPS", new PurchaseRequest("TrailTech", "GPS units", 12, new BigDecimal("249.00"), "Guided hikes"));
        ProcessView waiting = approvals.submit("12 GPS units from TrailTech at 249 each", ASHA);

        assertThatThrownBy(() -> approvals.decide(waiting.processId(), "E-17", true, true, "Looks fine to me"))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("own request");
        assertThatThrownBy(() -> approvals.decide(waiting.processId(), "E-22", false, true, "Sure"))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("budget holders");

        assertThat(approvals.status(waiting.processId()).status()).isEqualTo("WAITING");   // still waiting, untouched
    }

    @Test
    void aRejectionIsAnOutcomeToo() throws Exception {
        modelReads("GPS", new PurchaseRequest("TrailTech", "GPS units", 12, new BigDecimal("249.00"), "Guided hikes"));
        ProcessView waiting = approvals.submit("12 GPS units from TrailTech at 249 each", ASHA);

        ProcessView decided = approvals.decide(waiting.processId(), "M-3", true, false, "Get a second quote first");

        System.out.println("rejected: " + decided.result());
        assertThat(decided.result().status()).isEqualTo("REJECTED");
        assertThat(decided.result().comment()).isEqualTo("Get a second quote first");
    }
}
