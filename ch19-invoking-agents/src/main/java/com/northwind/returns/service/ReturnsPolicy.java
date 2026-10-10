package com.northwind.returns.service;

import com.northwind.returns.domain.Order;
import com.northwind.returns.domain.ReturnDecision;
import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** NorthWind's returns policy, as plain, testable Java. No model is involved in deciding. */
@Service
public class ReturnsPolicy {

    static final int RETURN_WINDOW_DAYS = 30;

    private final Clock clock;

    public ReturnsPolicy() {
        this(Clock.systemDefaultZone());
    }

    ReturnsPolicy(Clock clock) {
        this.clock = clock;
    }

    public ReturnDecision evaluate(ReturnRequest request, Order order) {
        var line = order.line(request.sku()).orElse(null);
        if (line == null) {
            return ReturnDecision.rejected("Item " + request.sku() + " is not part of order " + order.number());
        }
        boolean ourFault = request.reason() == ReturnReason.DAMAGED
                || request.reason() == ReturnReason.WRONG_ITEM
                || request.reason() == ReturnReason.NOT_AS_DESCRIBED;
        if (line.finalSale() && !ourFault) {
            return ReturnDecision.rejected("Final-sale items can only be returned if faulty or incorrect");
        }
        long days = ChronoUnit.DAYS.between(order.deliveredOn(), LocalDate.now(clock));
        if (days > RETURN_WINDOW_DAYS && !ourFault) {
            return ReturnDecision.rejected("The " + RETURN_WINDOW_DAYS + "-day return window has passed");
        }
        return ReturnDecision.approved(line.price(), ourFault,
                ourFault ? "We are sorry the item was not right" : "Returned within the return window");
    }
}
