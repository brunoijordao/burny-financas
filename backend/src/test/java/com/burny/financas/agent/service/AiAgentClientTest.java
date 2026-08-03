package com.burny.financas.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.burny.financas.agent.config.AiAgentProperties;
import com.burny.financas.agent.exception.AgentModelException;
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
 * Uses a plain JDK {@link HttpServer}, mirroring {@code GemmaClientTest} — no real network call to
 * Google AI Studio, no extra test-only HTTP mocking dependency.
 */
class AiAgentClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private AiAgentProperties propertiesFor(int port) {
        AiAgentProperties properties = new AiAgentProperties();
        properties.setBaseUrl("http://localhost:" + port);
        properties.setModel("test-model");
        properties.setApiKey("test-key");
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(2000);
        properties.setMaxHistoryMessages(40);
        return properties;
    }

    private HttpServer startServer(int status, String body) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", exchange -> respond(exchange, status, body));
        httpServer.start();
        return httpServer;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private List<GeminiApiTypes.Content> sampleContents() {
        return List.of(new GeminiApiTypes.Content("user", List.of(GeminiApiTypes.Part.ofText("qual meu saldo?"))));
    }

    @Test
    void parsesAFinalTextAnswer() throws IOException {
        server = startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Seu saldo e R$100\"}]}}]}");

        AiAgentClient client = new AiAgentClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        ModelTurn turn = client.generate(sampleContents(), "system instruction", List.of());

        assertThat(turn).isInstanceOf(ModelTurn.TextTurn.class);
        assertThat(((ModelTurn.TextTurn) turn).text()).isEqualTo("Seu saldo e R$100");
    }

    @Test
    void parsesAFunctionCall() throws IOException {
        server = startServer(200, "{\"candidates\":[{\"content\":{\"parts\":["
                + "{\"functionCall\":{\"name\":\"getBudgetStatus\",\"args\":{}}}"
                + "]}}]}");

        AiAgentClient client = new AiAgentClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        ModelTurn turn = client.generate(sampleContents(), "system instruction", List.of());

        assertThat(turn).isInstanceOf(ModelTurn.FunctionCallTurn.class);
        assertThat(((ModelTurn.FunctionCallTurn) turn).functionCall().name()).isEqualTo("getBudgetStatus");
    }

    @Test
    void skipsThoughtPartsAndUsesTheFirstRealAnswerPart() throws IOException {
        server = startServer(200, "{\"candidates\":[{\"content\":{\"parts\":["
                + "{\"text\":\"reasoning...\",\"thought\":true},"
                + "{\"text\":\"resposta final\"}"
                + "]}}]}");

        AiAgentClient client = new AiAgentClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());
        ModelTurn turn = client.generate(sampleContents(), "system instruction", List.of());

        assertThat(((ModelTurn.TextTurn) turn).text()).isEqualTo("resposta final");
    }

    @Test
    void nonTwoXxResponseThrowsAgentModelException() throws IOException {
        server = startServer(500, "{\"error\":\"internal\"}");

        AiAgentClient client = new AiAgentClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());

        assertThatThrownBy(() -> client.generate(sampleContents(), "system instruction", List.of()))
                .isInstanceOf(AgentModelException.class);
    }

    @Test
    void malformedEnvelopeThrowsAgentModelException() throws IOException {
        server = startServer(200, "not even json");

        AiAgentClient client = new AiAgentClient(propertiesFor(server.getAddress().getPort()), new ObjectMapper());

        assertThatThrownBy(() -> client.generate(sampleContents(), "system instruction", List.of()))
                .isInstanceOf(AgentModelException.class);
    }

    @Test
    void connectTimeoutThrowsAgentModelException() {
        AiAgentProperties properties = propertiesFor(1);
        properties.setConnectTimeoutMs(100);
        AiAgentClient client = new AiAgentClient(properties, new ObjectMapper());

        assertThatThrownBy(() -> client.generate(sampleContents(), "system instruction", List.of()))
                .isInstanceOf(AgentModelException.class);
    }
}
