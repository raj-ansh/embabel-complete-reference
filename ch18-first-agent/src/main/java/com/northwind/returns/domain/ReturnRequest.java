package com.northwind.returns.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** What the customer is asking for, in structured form. */
public record ReturnRequest(
        @NotBlank @Pattern(regexp = "NW-\\d{4,6}") String orderNumber,
        @NotBlank String sku,
        @NotNull ReturnReason reason,
        String customerWords) {          // a short quote of the customer's own explanation, if any

    public enum ReturnReason { DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, DOES_NOT_FIT, CHANGED_MIND }
}
