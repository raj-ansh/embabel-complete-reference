package com.northwind.content.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Structured analysis of one customer review: the "AI Text Analyzer". */
public record ReviewAnalysis(
        @NotNull Sentiment sentiment,
        @Min(1) @Max(5) Integer impliedRating,          // wrapper type: may be absent if unclear
        @NotNull List<Topic> topics,
        @NotBlank @Pattern(regexp = "[a-z]{2}") String language,
        @NotBlank @Size(max = 200) String summary,
        @NotNull Boolean mentionsPersonalData) {       // wrapper, not boolean: see Chapter 8 on Jackson 3

    public enum Sentiment { POSITIVE, NEUTRAL, NEGATIVE, MIXED }

    public enum Topic { QUALITY, SIZING, DELIVERY, PRICE, CUSTOMER_SERVICE, DURABILITY, OTHER }
}
