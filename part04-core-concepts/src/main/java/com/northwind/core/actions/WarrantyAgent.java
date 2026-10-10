package com.northwind.core.actions;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.SomeOf;
import com.embabel.agent.core.ActionException;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chapter 23: actions in depth.
 * - routing with a SomeOf return type (exactly one path is filled in)
 * - an optional (nullable) input that enriches an action when present
 * - a transient failure that Embabel retries
 */
@Agent(description = "Decide how to handle a warranty claim for NorthWind gear")
public class WarrantyAgent {

    public record WarrantyClaim(String claimId, String sku, LocalDate purchasedOn, boolean misuseSuspected) {
    }

    public record RepairPath(String claimId) {
    }

    public record ReplacementPath(String claimId) {
    }

    public record Rejection(String claimId, String reason) {
    }

    /** A union type: the planner continues with whichever field is non-null. */
    public record ClaimRoute(@Nullable RepairPath repair, @Nullable ReplacementPath replacement,
                             @Nullable Rejection rejection) implements SomeOf {
    }

    /** Optional extra information a caller may supply. */
    public record LoyaltyStatus(boolean goldMember) {
    }

    public record ClaimOutcome(String claimId, String outcome, int daysToResolve) {
    }

    private final RepairShopClient repairShop;

    public WarrantyAgent(RepairShopClient repairShop) {
        this.repairShop = repairShop;
    }

    @Action(description = "Route the claim by warranty rules")
    public ClaimRoute route(WarrantyClaim claim) {
        long ageDays = ChronoUnit.DAYS.between(claim.purchasedOn(), LocalDate.of(2026, 10, 7));
        if (claim.misuseSuspected()) {
            return new ClaimRoute(null, null, new Rejection(claim.claimId(), "Damage is not covered by warranty"));
        }
        if (ageDays <= 365) {
            return new ClaimRoute(null, new ReplacementPath(claim.claimId()), null);
        }
        if (ageDays <= 3 * 365) {
            return new ClaimRoute(new RepairPath(claim.claimId()), null, null);
        }
        return new ClaimRoute(null, null, new Rejection(claim.claimId(), "Outside the 3-year warranty"));
    }

    @AchievesGoal(description = "Warranty claim resolved by repair")
    @Action
    public ClaimOutcome repair(RepairPath path, @Nullable LoyaltyStatus loyalty) {
        int days = repairShop.bookSlot(path.claimId());                 // may fail transiently
        boolean gold = loyalty != null && loyalty.goldMember();
        return new ClaimOutcome(path.claimId(), "REPAIR" + (gold ? " (priority)" : ""), gold ? days / 2 : days);
    }

    @AchievesGoal(description = "Warranty claim resolved by replacement")
    @Action
    public ClaimOutcome replace(ReplacementPath path) {
        return new ClaimOutcome(path.claimId(), "REPLACEMENT", 3);
    }

    @AchievesGoal(description = "Warranty claim rejected")
    @Action
    public ClaimOutcome reject(Rejection rejection) {
        return new ClaimOutcome(rejection.claimId(), "REJECTED: " + rejection.reason(), 0);
    }

    /** A dependency that sometimes times out. Transient failures are retried by Embabel. */
    @org.springframework.stereotype.Component
    public static class RepairShopClient {

        private final AtomicInteger calls = new AtomicInteger();
        private volatile int failuresBeforeSuccess = 0;

        public int bookSlot(String claimId) {
            if (calls.incrementAndGet() <= failuresBeforeSuccess) {
                throw new RepairShopTimeout("Repair shop booking timed out for " + claimId);
            }
            return 10;
        }

        public void failNextCalls(int n) {
            calls.set(0);
            failuresBeforeSuccess = n;
        }

        public int calls() {
            return calls.get();
        }
    }

    public static class RepairShopTimeout extends ActionException.Transient {
        public RepairShopTimeout(String message) {
            super(message, null);          // Java must pass both arguments (no Kotlin defaults)
        }
    }
}
