package com.burny.financas.agent.service;

import com.burny.financas.agent.config.AiAgentProperties;
import com.burny.financas.agent.exception.AgentModelException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin synchronous client for Google AI Studio's Gemini {@code generateContent} REST endpoint.
 * Reuses the {@code RestClient}-with-explicit-timeouts-and-byte-response pattern established by
 * {@code GemmaClient} (design.md Decision 6), but is a distinct instance/bean with its own
 * {@link AiAgentProperties} — {@code GemmaClient}/{@code GemmaProperties} are never touched.
 *
 * <p>Unlike {@code GemmaClient}, a response can contain a {@code functionCall} part instead of
 * plain text (design.md Decision 1: Gemini's native function calling is used for intent
 * recognition instead of a hand-rolled JSON-intent prompt).
 */
@Component
@RequiredArgsConstructor
public class AiAgentClient {

    private final AiAgentProperties aiAgentProperties;
    private final ObjectMapper objectMapper;

    private RestClient restClient;

    public ModelTurn generate(
            List<GeminiApiTypes.Content> contents,
            String systemInstructionText,
            List<GeminiApiTypes.Tool> tools
    ) {
        GeminiApiTypes.GenerateContentRequest request = new GeminiApiTypes.GenerateContentRequest(
                contents,
                new GeminiApiTypes.Content(null, List.of(GeminiApiTypes.Part.ofText(systemInstructionText))),
                tools
        );

        try {
            // Read as raw bytes rather than String.class: Gemini has been observed responding with
            // Content-Type: application/octet-stream despite a JSON body (see GemmaClient), which
            // trips up Spring's message-converter negotiation for String. Bytes sidestep that.
            byte[] bytes = restClient().post()
                    .uri("/v1beta/models/{model}:generateContent?key={apiKey}",
                            aiAgentProperties.getModel(), aiAgentProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null) {
                throw new AgentModelException("Gemini returned an empty response body");
            }
            return parseTurn(new String(bytes, StandardCharsets.UTF_8));
        } catch (RestClientException e) {
            throw new AgentModelException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    private ModelTurn parseTurn(String responseBody) {
        try {
            GeminiApiTypes.GenerateContentResponse response =
                    objectMapper.readValue(responseBody, GeminiApiTypes.GenerateContentResponse.class);
            if (response.candidates() == null || response.candidates().isEmpty()) {
                throw new AgentModelException("Gemini returned no candidates");
            }
            List<GeminiApiTypes.Part> parts = response.candidates().get(0).content().parts();
            if (parts == null || parts.isEmpty()) {
                throw new AgentModelException("Gemini returned an empty response");
            }
            // Skip any chain-of-thought parts (thought=true) and use the first part that carries a
            // real answer — either a functionCall or non-thought text. See design.md Open Questions:
            // gemini-2.5-flash is not a "thinking" model by default, but this stays defensive in
            // case a thinking budget is ever configured, mirroring GemmaClient's precedent.
            for (GeminiApiTypes.Part part : parts) {
                if (Boolean.TRUE.equals(part.thought())) {
                    continue;
                }
                if (part.functionCall() != null) {
                    return new ModelTurn.FunctionCallTurn(part.functionCall());
                }
                if (part.text() != null) {
                    return new ModelTurn.TextTurn(part.text());
                }
            }
            throw new AgentModelException("Gemini did not return a usable answer part");
        } catch (AgentModelException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentModelException("Could not parse Gemini's response envelope", e);
        }
    }

    private RestClient restClient() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout((int) aiAgentProperties.getConnectTimeoutMs());
            requestFactory.setReadTimeout((int) aiAgentProperties.getReadTimeoutMs());

            restClient = RestClient.builder()
                    .baseUrl(aiAgentProperties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .build();
        }
        return restClient;
    }
}
