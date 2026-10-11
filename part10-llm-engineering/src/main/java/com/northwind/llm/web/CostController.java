package com.northwind.llm.web;

import com.northwind.llm.cost.CostLedger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chapter 80: the data behind a cost dashboard. Restrict it to operators in production (Chapter 84). */
@RestController
public class CostController {

    private final CostLedger ledger;

    public CostController(CostLedger ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/api/costs")
    public CostLedger.Report costs() {
        return ledger.report();
    }
}
