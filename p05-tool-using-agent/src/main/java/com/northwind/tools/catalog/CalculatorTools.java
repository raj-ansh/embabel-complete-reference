package com.northwind.tools.catalog;

import com.embabel.agent.api.annotation.LlmTool;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tool 1: exact arithmetic. Models are unreliable calculators; BigDecimal is not. */
public class CalculatorTools {

    @LlmTool(description = "Multiply a unit price by a quantity and return the exact total")
    public BigDecimal lineTotal(@LlmTool.Param(description = "Unit price, e.g. 189.00") BigDecimal unitPrice,
                                @LlmTool.Param(description = "Quantity, a whole number of at least 1") int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @LlmTool(description = "Apply a percentage discount to an amount and return the discounted amount")
    public BigDecimal applyDiscount(@LlmTool.Param(description = "Amount before discount") BigDecimal amount,
                                    @LlmTool.Param(description = "Discount percentage between 0 and 50") BigDecimal percent) {
        if (percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new IllegalArgumentException("discount must be between 0 and 50 percent");
        }
        BigDecimal factor = BigDecimal.ONE.subtract(percent.movePointLeft(2));
        return amount.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
