package com.burny.financas.transactions.exception;

/**
 * Reused for "not found" across the module's sub-resources (transaction, recurrence, attachment) —
 * same reuse convention already used by {@code CategoryNotFoundException} for categories/keywords.
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}
