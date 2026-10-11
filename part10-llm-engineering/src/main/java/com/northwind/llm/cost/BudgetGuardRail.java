package com.northwind.llm.cost;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationError;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;
import com.northwind.llm.domain.Tenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chapter 80: cost events arrive after a call, so they cannot stop it. This guardrail runs before the
 * next call: if the tenant has spent its budget, a CRITICAL error blocks the call (Chapter 81).
 */
@Component
public class BudgetGuardRail implements UserInputGuardRail {

    private final CostLedger ledger;
    private final TenantBudgets budgets;

    public BudgetGuardRail(CostLedger ledger, TenantBudgets budgets) {
        this.ledger = ledger;
        this.budgets = budgets;
    }

    @Override
    public String getName() {
        return "TenantBudget";
    }

    @Override
    public String getDescription() {
        return "Blocks model calls for tenants that have spent their monthly budget";
    }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        TenantContext who = blackboard.last(TenantContext.class);
        if (who == null) {
            return new ValidationResult(true, List.of());
        }
        double spent = ledger.spentBy(who.tenant());
        double limit = budgets.limitFor(who.tenant());
        if (spent < limit) {
            return new ValidationResult(true, List.of());
        }
        return new ValidationResult(false, List.of(new ValidationError("BUDGET_EXCEEDED",
                "Tenant %s has spent $%.6f of its $%.6f budget".formatted(who.tenant(), spent, limit),
                ValidationSeverity.CRITICAL)));
    }
}
