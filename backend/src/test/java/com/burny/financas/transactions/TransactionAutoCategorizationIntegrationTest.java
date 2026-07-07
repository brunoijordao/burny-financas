package com.burny.financas.transactions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionAutoCategorizationIntegrationTest {

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
                        .content("{\"name\":\"" + name + "\",\"icon\":\"utensils\",\"color\":\"#000\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void registerKeyword(String authHeader, long categoryId, String keyword) throws Exception {
        mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"" + keyword + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void descriptionMatchingKeywordAssignsCategoryAutomatically() throws Exception {
        String auth = authHeaderFor("tx-autocat-match@example.com");
        long accountId = createAccount(auth);
        long categoryId = createCategory(auth, "Alimentação");
        registerKeyword(auth, categoryId, "IFOOD");

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"IFOOD*IFOOD SAO PAULO\",\"amount\":30,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId", is((int) categoryId)));
    }

    @Test
    void descriptionMatchingNothingLeavesCategoryNull() throws Exception {
        String auth = authHeaderFor("tx-autocat-nomatch@example.com");
        long accountId = createAccount(auth);
        long categoryId = createCategory(auth, "Alimentação");
        registerKeyword(auth, categoryId, "IFOOD");

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"POSTO SHELL\",\"amount\":30,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId", nullValue()));
    }

    @Test
    void explicitCategoryBypassesAutomaticResolution() throws Exception {
        String auth = authHeaderFor("tx-autocat-explicit@example.com");
        long accountId = createAccount(auth);
        long foodCategoryId = createCategory(auth, "Alimentação");
        registerKeyword(auth, foodCategoryId, "IFOOD");
        long leisureCategoryId = createCategory(auth, "Lazer");

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"IFOOD*IFOOD SAO PAULO\",\"amount\":30,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId
                                + ",\"categoryId\":" + leisureCategoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId", is((int) leisureCategoryId)));
    }
}
