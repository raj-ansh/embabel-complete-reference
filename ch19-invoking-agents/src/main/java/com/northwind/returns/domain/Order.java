package com.northwind.returns.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** An order as NorthWind's order system knows it. */
public record Order(String number, String customerFirstName, LocalDate deliveredOn, List<Line> lines) {

    public record Line(String sku, String name, BigDecimal price, boolean finalSale) {
    }

    public java.util.Optional<Line> line(String sku) {
        return lines.stream().filter(l -> l.sku().equalsIgnoreCase(sku)).findFirst();
    }
}
