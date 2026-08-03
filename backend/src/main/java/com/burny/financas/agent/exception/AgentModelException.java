package com.burny.financas.agent.exception;

/**
 * Raised when the call to Gemini fails, times out, or returns a response that can't be parsed.
 * Always caught centrally (see {@code GlobalExceptionHandler}) and turned into a clear error
 * response for that chat turn — never allowed to propagate as an unhandled server error, and never
 * lets a failed model call create a transaction (spec.md "Agent Failure Handling").
 */
public class AgentModelException extends RuntimeException {
    public AgentModelException(String message) {
        super(message);
    }

    public AgentModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
