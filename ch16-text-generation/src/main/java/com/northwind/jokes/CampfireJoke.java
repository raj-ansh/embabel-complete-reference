package com.northwind.jokes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A joke with a structure we can check, store and format. */
public record CampfireJoke(
        @NotBlank @Size(max = 200) String setup,
        @NotBlank @Size(max = 120) String punchline) {
}
