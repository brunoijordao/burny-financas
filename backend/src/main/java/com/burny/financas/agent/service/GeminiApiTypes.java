package com.burny.financas.agent.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Wire-format records for Gemini's {@code generateContent} REST endpoint, shared by
 * {@link AiAgentClient} (builds requests / parses responses), {@link AiAgentChatService} (builds
 * the per-turn {@code contents} list), and {@code AgentToolCatalog} (builds {@code tools}) — unlike
 * {@code GemmaClient}'s one-shot prompt, this call is multi-turn with function calling, so the
 * shapes are used from more than one class.
 */
public final class GeminiApiTypes {

    private GeminiApiTypes() {
    }

    /** Exactly one of {@code text}/{@code functionCall}/{@code functionResponse} is populated per part. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text, FunctionCall functionCall, FunctionResponse functionResponse, Boolean thought) {
        public static Part ofText(String text) {
            return new Part(text, null, null, null);
        }

        public static Part ofFunctionCall(FunctionCall call) {
            return new Part(null, call, null, null);
        }

        public static Part ofFunctionResponse(FunctionResponse response) {
            return new Part(null, null, response, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(String role, List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(String name, Map<String, Object> args) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionResponse(String name, Map<String, Object> response) {
    }

    /** {@code parameters} is an OpenAPI-schema-subset object ({@code {"type":"OBJECT","properties":{...}}}). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionDeclaration(String name, String description, Map<String, Object> parameters) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tool(List<FunctionDeclaration> functionDeclarations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenerateContentRequest(List<Content> contents, Content systemInstruction, List<Tool> tools) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenerateContentResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {
    }
}
