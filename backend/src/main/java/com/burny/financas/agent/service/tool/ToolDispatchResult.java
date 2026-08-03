package com.burny.financas.agent.service.tool;

import com.burny.financas.agent.dto.TransactionDraft;
import java.util.Map;

/**
 * {@code functionResponsePayload} is always sent back to Gemini as the tool's {@code
 * functionResponse}. {@code draft} is non-null only for a successful {@code proposeTransaction}
 * call and is surfaced to the frontend alongside the model's final reply.
 */
public record ToolDispatchResult(Map<String, Object> functionResponsePayload, TransactionDraft draft) {
    public static ToolDispatchResult of(Map<String, Object> payload) {
        return new ToolDispatchResult(payload, null);
    }

    public static ToolDispatchResult withDraft(Map<String, Object> payload, TransactionDraft draft) {
        return new ToolDispatchResult(payload, draft);
    }
}
