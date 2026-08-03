package com.burny.financas.agent.exception;

/** Raised when a chat request fails a validation rule not expressible as a simple Bean Validation annotation (e.g. the configurable history-size cap). */
public class InvalidChatRequestException extends RuntimeException {
    public InvalidChatRequestException(String message) {
        super(message);
    }
}
