package com.burny.financas.agent;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.agent.service.AiAgentClient;
import com.burny.financas.agent.service.GeminiApiTypes;
import com.burny.financas.agent.service.ModelTurn;
import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies spec.md "Tool Execution Is Isolated To The Authenticated User": neither a tool call nor
 * the confirmation endpoint ever reads or writes another user's data, even when an id belonging to
 * another user is supplied (as a mocked model function-call argument, or directly in the confirm
 * request body).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentIsolationIntegrationTest {

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
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void confirmingADraftReferencingAnotherUsersAccountIsRejected() throws Exception {
        String ownerAuth = authHeaderFor("agent-iso-owner@example.com");
        long ownerAccountId = createAccount(ownerAuth);

        String otherAuth = authHeaderFor("agent-iso-other@example.com");
        String confirmBody = "{\"description\":\"Fast food\",\"amount\":25.00,\"type\":\"EXPENSE\","
                + "\"date\":\"2026-01-15\",\"accountId\":" + ownerAccountId + "}";

        mockMvc.perform(post("/ai-agent/transactions/confirm")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isNotFound());

        // The owner's account balance is untouched by the other user's attempt.
        mockMvc.perform(get("/accounts/" + ownerAccountId).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0)));
    }

    @Test
    void aFunctionCallArgumentReferencingAnotherUsersAccountNeverProducesADraft() throws Exception {
        String ownerAuth = authHeaderFor("agent-iso-toolcall-owner@example.com");
        long ownerAccountId = createAccount(ownerAuth);

        String attackerAuth = authHeaderFor("agent-iso-toolcall-attacker@example.com");

        // Simulates the model (hallucinating or otherwise) proposing a transaction against an
        // account id that belongs to a different user than the one authenticated on this request.
        GeminiApiTypes.FunctionCall functionCall = new GeminiApiTypes.FunctionCall("proposeTransaction", Map.of(
                "accountId", String.valueOf(ownerAccountId),
                "type", "EXPENSE",
                "amount", 999,
                "description", "tentativa maliciosa"
        ));
        when(aiAgentClient.generate(any(), any(), any()))
                .thenReturn(new ModelTurn.FunctionCallTurn(functionCall))
                .thenReturn(new ModelTurn.TextTurn("Nao encontrei essa conta."));

        mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, attackerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"gastei 999 na conta dele\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft", is(nullValue())));

        // The account owner's balance is untouched.
        mockMvc.perform(get("/accounts/" + ownerAccountId).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0)));
    }
}
