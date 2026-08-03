package com.burny.financas.agent.exception;

/**
 * Reserved for a genuinely unexpected failure inside a tool handler. Expected validation failures
 * (unknown tool name, an account/category id that doesn't belong to the caller, missing required
 * arguments) are NOT this exception — {@code AgentToolDispatcher} turns those into an error
 * {@code functionResponse} so Gemini can recover on its next turn (design.md Decision 3), rather
 * than throwing.
 */
public class AgentToolExecutionException extends RuntimeException {
    public AgentToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
