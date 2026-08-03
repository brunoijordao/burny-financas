package com.burny.financas.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One turn of the client-resent conversation history (see design.md Non-Goals: no server-side persistence). */
public record ChatMessage(
        @NotNull(message = "Role is required")
        ChatRole role,

        @NotBlank(message = "Text is required")
        @Size(max = 4000, message = "Text must be at most 4000 characters")
        String text
) {
}
