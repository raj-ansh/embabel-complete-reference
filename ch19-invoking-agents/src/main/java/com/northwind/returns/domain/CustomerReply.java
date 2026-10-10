package com.northwind.returns.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The message sent back to the customer: the agent's goal. */
public record CustomerReply(@NotBlank @Size(max = 120) String subject, @NotBlank @Size(max = 1200) String body) {
}
