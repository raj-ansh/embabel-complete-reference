package com.northwind.content.copy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** What the merchandising team provides. Validated before any model is called. */
public record ProductBrief(
        @NotBlank String sku,
        @NotBlank @Size(max = 80) String name,
        @NotBlank String category,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 120) String> features,
        @NotBlank String audience) {
}
