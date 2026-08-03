package com.burny.financas.agent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code history} defaults to an empty list when omitted. Its size is capped here as a hard ceiling
 * regardless of {@code app.ai-agent.max-history-messages} (checked again, configurably, in
 * {@code AiAgentChatService} — see design.md Decision 8: the frontend's cap is a UX default, not a
 * security boundary, so the backend never trusts it alone).
 */
public record ChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message must be at most 4000 characters")
        String message,

        @Size(max = 200, message = "Conversation history is too long")
        List<@Valid ChatMessage> history
) {
    public ChatRequest {
        history = history == null ? List.of() : history;
    }
}
