package com.northwind.tools;

import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.ToolCallContext;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.tools.data.InventoryReportTools;
import com.northwind.tools.data.ProductRepository;
import com.northwind.tools.data.ReservationTools;
import com.northwind.tools.security.GuardedTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapters 46 and 47: a database write tool, and the guard that wraps it. */
class WriteToolsAndSecurityTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    ReservationTools reservations;

    @Autowired
    ProductRepository products;

    @Autowired
    InventoryReportTools reports;

    @Test
    void jdbcReportsUseFixedSqlAndBoundedParameters() {
        var lowest = Tool.fromInstance(reports).stream()
                .filter(t -> t.getDefinition().getName().equals("lowStock")).findFirst().orElseThrow();

        System.out.println("stockByCategory: " + reports.stockByCategory());
        System.out.println("lowStock(10):    " + text(lowest.call("{\"threshold\": 10}")));

        assertThat(reports.stockByCategory()).extracting(InventoryReportTools.CategoryStock::category)
                .containsExactly("footwear", "outerwear", "tents");
        assertThat(text(lowest.call("{\"threshold\": 10}"))).contains("TENT-4P").doesNotContain("BOOT-W9");
        assertThat(text(lowest.call("{\"threshold\": 100000}"))).contains("BOOT-W9");   // clamped to 100, still bounded
    }

    private Tool hold() {
        return Tool.fromInstance(reservations).getFirst();
    }

    private static ToolCallContext signedIn(String customerId) {
        return ToolCallContext.of(Map.of("customerId", customerId, "roles", Set.of("customer")));
    }

    private static String text(Tool.Result result) {
        return switch (result) {
            case Tool.Result.Text t -> t.getContent();
            case Tool.Result.WithArtifact a -> a.getContent();
            default -> result.toString();
        };
    }

    @Test
    void holdsAreBoundedIdempotentAndTransactional() {
        int before = products.findById("JKT-GL").orElseThrow().getStock();

        String first = text(hold().call("""
                {"sku": "JKT-GL", "quantity": 2}""", signedIn("C-2001")));
        String repeat = text(hold().call("""
                {"sku": "JKT-GL", "quantity": 2}""", signedIn("C-2001")));
        String tooMany = text(hold().call("""
                {"sku": "BOOT-W9", "quantity": 4}""", signedIn("C-2001")));
        String outOfStock = text(hold().call("""
                {"sku": "TENT-4P", "quantity": 1}""", signedIn("C-2001")));

        System.out.println("first:      " + first);
        System.out.println("repeat:     " + repeat);
        System.out.println("tooMany:    " + tooMany);
        System.out.println("outOfStock: " + outOfStock);

        assertThat(first).contains("\"created\":true");
        assertThat(repeat).contains("\"created\":false", "already have this item on hold");
        assertThat(tooMany).contains("limited to 1-3");
        assertThat(outOfStock).contains("Only 0 in stock");
        assertThat(products.findById("JKT-GL").orElseThrow().getStock()).isEqualTo(before - 2);   // once, not twice
    }

    @Test
    void customerComesFromContextNotFromTheModel() {
        String schema = hold().getDefinition().getInputSchema().toJsonSchema();

        System.out.println("holdForPickup schema: " + schema);
        assertThat(schema).contains("sku", "quantity").doesNotContain("customerId");
    }

    @Test
    void guardDeniesCallersWithoutTheRole() {
        Tool guarded = new GuardedTool(hold(), "customer", 5);

        String anonymous = text(guarded.call("""
                {"sku": "BOOT-W9", "quantity": 1}""", ToolCallContext.of(Map.of("customerId", "C-2002"))));

        assertThat(anonymous).isEqualTo("This action is not available for your account.");
        assertThat(guarded.getDefinition().getName()).isEqualTo("holdForPickup");   // the model sees the same tool
    }

    @Test
    void guardEnforcesAPerCustomerCallBudget() {
        var clock = new MutableClock(Instant.parse("2026-10-07T09:15:00Z"));
        Tool guarded = new GuardedTool(hold(), "customer", 2, clock);
        String call = """
                {"sku": "BOOT-W9", "quantity": 1}""";

        guarded.call(call, signedIn("C-2003"));
        guarded.call(call, signedIn("C-2003"));
        String third = text(guarded.call(call, signedIn("C-2003")));

        assertThat(third).startsWith("Too many requests");

        clock.advance(Duration.ofHours(1));                                        // a new hour, a new budget
        assertThat(text(guarded.call(call, signedIn("C-2003")))).doesNotStartWith("Too many requests");
    }

    @Test
    void guardTurnsFailuresIntoSafeMessages() {
        Tool failing = Tool.create("explode", "Always fails", input -> {
            throw new IllegalStateException("db password=hunter2 rejected");
        });
        Tool.Result result = new GuardedTool(failing, "customer", 5).call("{}", signedIn("C-2004"));

        System.out.println("guarded failure: " + result);
        assertThat(result).isInstanceOf(Tool.Result.Error.class);
        assertThat(result.toString()).doesNotContain("hunter2");
    }

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) { this.now = start; }

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }

        @Override public Clock withZone(ZoneId zone) { return this; }

        @Override public Instant instant() { return now; }
    }
}
