package com.northwind.content.extract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** An order number in NorthWind's format, e.g. NW-10442. */
public record OrderReference(@NotBlank @Pattern(regexp = "NW-\\d{4,6}") String orderNumber) {
}
