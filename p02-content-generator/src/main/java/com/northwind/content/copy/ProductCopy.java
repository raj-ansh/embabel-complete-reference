package com.northwind.content.copy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The structure the model must fill in. The constraints are checked after the model replies;
 * a violation makes Embabel retry the call, telling the model what was wrong.
 */
public record ProductCopy(
        @NotBlank @Size(max = 60) String headline,
        @NotBlank @Size(min = 80, max = 400) String description,
        @NotNull @Size(min = 3, max = 5) List<@NotBlank @Size(max = 90) String> bulletPoints,
        @NotNull @Size(max = 8) List<@NotBlank String> searchKeywords,
        @NotNull Tone tone) {

    public enum Tone { ADVENTUROUS, PRACTICAL, PREMIUM, FAMILY }
}
