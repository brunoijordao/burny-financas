package com.burny.financas.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
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
import java.math.BigDecimal;
import java.util.List;
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

/** {@link AiAgentClient} is mocked (no real call to Google AI Studio); everything downstream of it (tool dispatch, data, transaction creation, balance effect) runs for real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentHappyPathIntegrationTest {

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

    private long createCategory(String authHeader, String name) throws Exception {
        String response = mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"icon\":\"tag\",\"color\":\"#123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void createTransaction(String authHeader, long accountId, long categoryId, String amount) throws Exception {
        String body = "{\"description\":\"Uber\",\"amount\":" + amount + ",\"type\":\"EXPENSE\","
                + "\"transactionDate\":\"" + java.time.LocalDate.now() + "\",\"accountId\":" + accountId
                + ",\"categoryId\":" + categoryId + ",\"recurring\":false}";
        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void spendingByCategoryQueryIsAnsweredWithRealData() throws Exception {
        String auth = authHeaderFor("agent-happy-query@example.com");
        long accountId = createAccount(auth);
        long categoryId = createCategory(auth, "Transporte");
        createTransaction(auth, accountId, categoryId, "80.00");

        GeminiApiTypes.FunctionCall functionCall = new GeminiApiTypes.FunctionCall("getSpendingByCategory", Map.of());
        when(aiAgentClient.generate(any(), any(), any()))
                .thenReturn(new ModelTurn.FunctionCallTurn(functionCall))
                .thenReturn(new ModelTurn.TextTurn("Voce gastou R$80,00 com transporte este mes."));

        String response = mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"quanto gastei com transporte esse mes?\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(response).get("reply").asText()).contains("80,00");

        // Verify the SECOND call to Gemini actually carried the real, computed spending figure back
        // as the functionResponse — i.e. the reply is grounded in real data, not just a fixed mock text.
        org.mockito.ArgumentCaptor<List<GeminiApiTypes.Content>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(aiAgentClient, org.mockito.Mockito.times(2)).generate(captor.capture(), any(), any());
        List<GeminiApiTypes.Content> secondCallContents = captor.getAllValues().get(1);
        GeminiApiTypes.Content lastContent = secondCallContents.get(secondCallContents.size() - 1);
        GeminiApiTypes.FunctionResponse functionResponse = lastContent.parts().get(0).functionResponse();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) functionResponse.response().get("categories");
        assertThat(categories).anySatisfy(category -> {
            assertThat(category.get("categoryName")).isEqualTo("Transporte");
            assertThat(new BigDecimal(category.get("total").toString())).isEqualByComparingTo("80.00");
        });
    }

    @Test
    void transactionDescriptionProducesADraftAndConfirmingItCreatesTheTransaction() throws Exception {
        String auth = authHeaderFor("agent-happy-write@example.com");
        long accountId = createAccount(auth);

        GeminiApiTypes.FunctionCall functionCall = new GeminiApiTypes.FunctionCall("proposeTransaction", Map.of(
                "accountId", String.valueOf(accountId),
                "type", "EXPENSE",
                "amount", 100,
                "description", "fast food"
        ));
        when(aiAgentClient.generate(any(), any(), any()))
                .thenReturn(new ModelTurn.FunctionCallTurn(functionCall))
                .thenReturn(new ModelTurn.TextTurn("Confirma o lancamento de R$100,00 em fast food?"));

        String chatResponse = mockMvc.perform(post("/ai-agent/messages")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"gastei 100 com fast food na conta corrente\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.accountId", is((int) accountId)))
                .andExpect(jsonPath("$.draft.amount", is(100.0)))
                .andReturn().getResponse().getContentAsString();

        var draftNode = objectMapper.readTree(chatResponse).get("draft");
        assertThat(draftNode.get("description").asText()).isEqualTo("fast food");

        String confirmBody = "{\"description\":\"fast food\",\"amount\":100,\"type\":\"EXPENSE\","
                + "\"date\":\"" + draftNode.get("date").asText() + "\",\"accountId\":" + accountId + "}";
        mockMvc.perform(post("/ai-agent/transactions/confirm")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(100)))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        mockMvc.perform(get("/accounts/" + accountId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(-100)));
    }
}
