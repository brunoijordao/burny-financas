package com.burny.financas.agent.dto;

/** {@code draft} is non-null only when this turn's reply is accompanied by a proposed transaction. */
public record ChatResponse(
        String reply,
        TransactionDraft draft
) {
}
