package com.burny.financas.pdfimport.service;

import com.burny.financas.pdfimport.config.GemmaProperties;
import com.burny.financas.pdfimport.exception.PdfInterpretationException;
import com.burny.financas.transactions.entity.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin synchronous client for Google AI Studio's Gemma {@code generateContent} REST endpoint (see
 * design.md "Decision 6": {@code RestClient}, not WebClient, since this call already runs off the
 * request thread inside the {@code @Async} PDF-import pipeline). Connect/read timeouts come from
 * {@link GemmaProperties} so a slow or hanging provider can't block the async worker indefinitely.
 */
@Component
@RequiredArgsConstructor
public class GemmaClient {

    private static final String PROMPT_TEMPLATE = """
            You are given the raw text extracted from a page of a Brazilian bank (Itau) account \
            statement PDF. Identify every individual transaction (purchases, payments, transfers, \
            deposits, fees, etc.) described in the text.

            Respond with ONLY a JSON array (no markdown fences, no commentary) where each element has \
            exactly these fields:
            - "date": the transaction date in ISO 8601 format (yyyy-MM-dd)
            - "description": the transaction description as it appears in the statement
            - "amount": the transaction amount as a positive number (never negative)
            - "type": either "INCOME" or "EXPENSE"
            - "category": your best guess at a short category name for this transaction (e.g. \
            "Alimentacao", "Transporte", "Salario"), or null if you are not confident

            If no transactions can be identified, respond with an empty JSON array: []

            Statement text:
            ---
            %s
            ---
            """;

    private final GemmaProperties gemmaProperties;
    private final ObjectMapper objectMapper;

    private RestClient restClient;

    public List<GemmaCandidateTransaction> interpret(String statementText) {
        String responseBody = callGemma(buildPrompt(statementText));
        String jsonArrayText = extractResponseText(responseBody);
        return parseCandidates(jsonArrayText);
    }

    private String buildPrompt(String statementText) {
        return PROMPT_TEMPLATE.formatted(statementText);
    }

    private String callGemma(String prompt) {
        try {
            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))),
                    new GeminiRequest.GenerationConfig("application/json")
            );

            // Read as raw bytes rather than String.class: Gemini has been observed responding with
            // Content-Type: application/octet-stream despite a JSON body, which Spring's String
            // message converter negotiation rejects. Bytes sidestep that negotiation entirely.
            byte[] bytes = restClient().post()
                    .uri("/v1beta/models/{model}:generateContent?key={apiKey}",
                            gemmaProperties.getModel(), gemmaProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null) {
                throw new PdfInterpretationException("Gemma returned an empty response body");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (RestClientException e) {
            throw new PdfInterpretationException("Gemma API call failed: " + e.getMessage(), e);
        }
    }

    private String extractResponseText(String responseBody) {
        try {
            GeminiResponse response = objectMapper.readValue(responseBody, GeminiResponse.class);
            if (response.candidates() == null || response.candidates().isEmpty()) {
                throw new PdfInterpretationException("Gemma returned no candidates");
            }
            List<GeminiResponse.Part> parts = response.candidates().get(0).content().parts();
            if (parts == null || parts.isEmpty()) {
                throw new PdfInterpretationException("Gemma returned an empty response");
            }
            // "Thinking" models (this one included) emit one or more chain-of-thought parts
            // (thought=true) before the actual answer part; the answer is always last.
            GeminiResponse.Part finalPart = parts.get(parts.size() - 1);
            if (Boolean.TRUE.equals(finalPart.thought()) || finalPart.text() == null) {
                throw new PdfInterpretationException("Gemma did not return a final answer part");
            }
            return finalPart.text();
        } catch (PdfInterpretationException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfInterpretationException("Could not parse Gemma's response envelope", e);
        }
    }

    private List<GemmaCandidateTransaction> parseCandidates(String jsonArrayText) {
        try {
            List<CandidateJson> raw = objectMapper.readValue(jsonArrayText, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, CandidateJson.class));
            return raw.stream()
                    .filter(CandidateJson::isUsable)
                    .map(CandidateJson::toCandidate)
                    .toList();
        } catch (Exception e) {
            throw new PdfInterpretationException("Gemma returned malformed transaction data", e);
        }
    }

    private RestClient restClient() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout((int) gemmaProperties.getConnectTimeoutMs());
            requestFactory.setReadTimeout((int) gemmaProperties.getReadTimeoutMs());

            restClient = RestClient.builder()
                    .baseUrl(gemmaProperties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .build();
        }
        return restClient;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CandidateJson(String date, String description, BigDecimal amount, String type, String category) {
        boolean isUsable() {
            return date != null && description != null && amount != null && type != null
                    && (type.equalsIgnoreCase("INCOME") || type.equalsIgnoreCase("EXPENSE"));
        }

        GemmaCandidateTransaction toCandidate() {
            return new GemmaCandidateTransaction(
                    LocalDate.parse(date),
                    description,
                    amount.abs(),
                    TransactionType.valueOf(type.toUpperCase()),
                    (category == null || category.isBlank()) ? null : category
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Content(List<Part> parts) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Part(String text) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record GenerationConfig(String responseMimeType) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<Candidate> candidates) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Candidate(Content content) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Content(List<Part> parts) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Part(String text, Boolean thought) {
        }
    }
}
