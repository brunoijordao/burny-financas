package com.burny.financas.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.agent.service.AiAgentClient;
import com.burny.financas.agent.service.ModelTurn;
import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Small isolated capacity via {@code @TestPropertySource} (own Spring context), mirroring {@code
 * PdfImportRateLimitIntegrationTest}. {@link AiAgentClient} is mocked so no real call to Google AI
 * Studio is made and the reply is deterministic/instant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.ai-agent.capacity=3",
        "app.rate-limit.ai-agent.refill-period-seconds=3600"
})
class AiAgentRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AiAgentClient aiAgentClient;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private long createAccount(String authHeader) throws Exception {
        String response = mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conta\",\"icon\":\"wallet\",\"color\":\"#000\",\"type\":\"CHECKING\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void sendMessage(String authHeader) throws Exception {
        mockMvc.perform(post("/ai-agent/messages")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"oi\",\"history\":[]}"));
    }

    @Test
    void messagesOverLimitReturns429WithRetryAfter() throws Exception {
        when(aiAgentClient.generate(any(), any(), any())).thenReturn(new ModelTurn.TextTurn("ola"));
        String auth = authHeaderFor("agent-rate-over-limit@example.com");

        for (int i = 0; i < 3; i++) {
            sendMessage(auth);
        }

        mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"oi de novo\",\"history\":[]}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void messagesRateLimitIsScopedPerUserNotGlobal() throws Exception {
        when(aiAgentClient.generate(any(), any(), any())).thenReturn(new ModelTurn.TextTurn("ola"));
        String authA = authHeaderFor("agent-rate-user-a@example.com");
        for (int i = 0; i < 3; i++) {
            sendMessage(authA);
        }
        mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"oi\",\"history\":[]}"))
                .andExpect(status().isTooManyRequests());

        String authB = authHeaderFor("agent-rate-user-b@example.com");
        mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, authB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"oi\",\"history\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmingADraftDoesNotConsumeTheChatRateLimitBucket() throws Exception {
        when(aiAgentClient.generate(any(), any(), any())).thenReturn(new ModelTurn.TextTurn("ola"));
        String auth = authHeaderFor("agent-rate-confirm-exempt@example.com");
        long accountId = createAccount(auth);

        for (int i = 0; i < 3; i++) {
            sendMessage(auth);
        }
        // Chat bucket is now exhausted for this user...
        mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"oi\",\"history\":[]}"))
                .andExpect(status().isTooManyRequests());

        // ...but confirming a draft transaction (never calls Gemini) is unaffected.
        String confirmBody = "{\"description\":\"Fast food\",\"amount\":25.00,\"type\":\"EXPENSE\","
                + "\"date\":\"2026-01-15\",\"accountId\":" + accountId + "}";
        mockMvc.perform(post("/ai-agent/transactions/confirm")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk());
    }
}
