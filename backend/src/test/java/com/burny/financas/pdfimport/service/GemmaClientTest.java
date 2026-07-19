package com.burny.financas.pdfimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.burny.financas.pdfimport.config.GemmaProperties;
import com.burny.financas.pdfimport.exception.PdfInterpretationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Uses a plain JDK {@link HttpServer} instead of a mocking library so the Gemini wire format can be
 * exercised end-to-end (prompt request out, JSON response in) without a real network dependency or
 * an extra test-only HTTP mocking dependency.
 */
class GemmaClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private GemmaProperties propertiesFor(int port) {
        GemmaProperties properties = new GemmaProperties();
        properties.setBaseUrl("http://localhost:" + port);
        properties.setModel("test-model");
        properties.setApiKey("test-key");
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(2000);
        return properties;
    }

    private HttpServer startServer(int status, String body) throws IOException {
        return startServer(status, body, "application/json");
    }

    private HttpServer startServer(int status, String body, String contentType) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", exchange -> respond(exchange, status, body, contentType));
        httpServer.start();
        return httpServer;
    }

    private void respond(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String geminiEnvelope(String innerJsonArray) {
        // Mirrors the actual Gemini generateContent response shape: the model's answer is itself a
        // JSON string nested inside candidates[0].content.parts[0].text.
        String escapedInner = innerJsonArray.replace("\"", "\\\"");
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + escapedInner + "\"}]}}]}";
    }

    @Test
    void skipsThinkingPartsAndUsesTheFinalAnswerPart() throws IOException {
        // "Thinking" models (gemma-4-31b-it included) emit one or more chain-of-thought parts
        // (thought=true) before the actual answer part, which is always last and has no "thought"
        // field at all — mirrors the real response captured against the live API.
        String innerArray = "[{\"date\":\"2026-01-15\",\"description\":\"COMPRA SUPERMERCADO\","
                + "\"amount\":123.45,\"type\":\"EXPENSE\",\"category\":null}]";
        String escapedInner = innerArray.replace("\"", "\\\"");
        String body = "{\"candidates\":[{\"content\":{\"parts\":["
                + "{\"text\":\"reasoning about the task...\",\"thought\":true},"
                + "{\"text\":\"" + escapedInner + "\"}"
                + "]}}]}";
        server = startServer(200, body);

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        List<GemmaCandidateTransaction> result = client.interpret("some statement text");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("COMPRA SUPERMERCADO");
    }

    @Test
    void survivesResponseServedAsApplicationOctetStream() throws IOException {
        // Observed against the live API: Gemini sometimes serves a JSON body with
        // Content-Type: application/octet-stream, which Spring's String.class response conversion
        // rejects outright — the client must read raw bytes instead to tolerate this.
        String innerArray = "[{\"date\":\"2026-01-15\",\"description\":\"COMPRA SUPERMERCADO\","
                + "\"amount\":123.45,\"type\":\"EXPENSE\",\"category\":null}]";
        server = startServer(200, geminiEnvelope(innerArray), "application/octet-stream");

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        List<GemmaCandidateTransaction> result = client.interpret("some statement text");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("COMPRA SUPERMERCADO");
    }

    @Test
    void parsesSuccessfulResponseIntoCandidates() throws IOException {
        String innerArray = "[{\"date\":\"2026-01-15\",\"description\":\"COMPRA SUPERMERCADO\","
                + "\"amount\":123.45,\"type\":\"EXPENSE\",\"category\":\"Alimentacao\"}]";
        server = startServer(200, geminiEnvelope(innerArray));

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        List<GemmaCandidateTransaction> result = client.interpret("some statement text");

        assertThat(result).hasSize(1);
        GemmaCandidateTransaction candidate = result.get(0);
        assertThat(candidate.description()).isEqualTo("COMPRA SUPERMERCADO");
        assertThat(candidate.amount()).isEqualByComparingTo("123.45");
        assertThat(candidate.suggestedCategoryName()).isEqualTo("Alimentacao");
    }

    @Test
    void skipsIndividualMalformedEntriesWithoutFailingTheWholeImport() throws IOException {
        String innerArray = "["
                + "{\"date\":\"2026-01-15\",\"description\":\"OK\",\"amount\":10,\"type\":\"EXPENSE\",\"category\":null},"
                + "{\"date\":null,\"description\":\"MISSING DATE\",\"amount\":10,\"type\":\"EXPENSE\"}"
                + "]";
        server = startServer(200, geminiEnvelope(innerArray));

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        List<GemmaCandidateTransaction> result = client.interpret("some statement text");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("OK");
    }

    @Test
    void nonTwoXxResponseThrowsPdfInterpretationException() throws IOException {
        server = startServer(500, "{\"error\":\"internal\"}");

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());

        assertThatThrownBy(() -> client.interpret("some statement text"))
                .isInstanceOf(PdfInterpretationException.class);
    }

    @Test
    void malformedEnvelopeThrowsPdfInterpretationException() throws IOException {
        server = startServer(200, "not even json");

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());

        assertThatThrownBy(() -> client.interpret("some statement text"))
                .isInstanceOf(PdfInterpretationException.class);
    }

    @Test
    void malformedInnerJsonArrayThrowsPdfInterpretationException() throws IOException {
        server = startServer(200, geminiEnvelope("this is not a json array"));

        GemmaClient client = new GemmaClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());

        assertThatThrownBy(() -> client.interpret("some statement text"))
                .isInstanceOf(PdfInterpretationException.class);
    }
}
