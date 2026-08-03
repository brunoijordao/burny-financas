package com.burny.financas.agent.service;

/** A single Gemini turn: either a final natural-language answer, or a request to call a tool. */
public sealed interface ModelTurn {
    record TextTurn(String text) implements ModelTurn {
    }

    record FunctionCallTurn(GeminiApiTypes.FunctionCall functionCall) implements ModelTurn {
    }
}
