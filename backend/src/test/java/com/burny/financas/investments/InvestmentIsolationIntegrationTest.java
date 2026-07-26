package com.burny.financas.investments;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
class InvestmentIsolationIntegrationTest {

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

    private long createAsset(String authHeader) throws Exception {
        String response = mockMvc.perform(post("/investments/assets")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PETR4\",\"ticker\":\"PETR4\",\"type\":\"STOCK\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createOperation(String authHeader, long assetId) throws Exception {
        String body = String.format(
                "{\"type\":\"BUY\",\"quantity\":10,\"unitPrice\":50.00,\"operationDate\":\"%s\"}", LocalDate.now());
        String response = mockMvc.perform(post("/investments/assets/" + assetId + "/operations")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersAsset() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-get-owner@example.com");
        long assetId = createAsset(ownerAuth);

        String otherAuth = authHeaderFor("inv-iso-get-other@example.com");
        mockMvc.perform(get("/investments/assets/" + assetId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnAssets() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-list-owner@example.com");
        createAsset(ownerAuth);

        String otherAuth = authHeaderFor("inv-iso-list-other@example.com");
        mockMvc.perform(get("/investments/assets").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        mockMvc.perform(get("/investments/assets").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void nonOwnerCannotUpdateOrDeleteAnotherUsersAsset() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-actions-owner@example.com");
        long assetId = createAsset(ownerAuth);

        String otherAuth = authHeaderFor("inv-iso-actions-other@example.com");

        mockMvc.perform(put("/investments/assets/" + assetId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"type\":\"STOCK\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/investments/assets/" + assetId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonOwnerCannotCreateOrListOperationsAgainstAnotherUsersAsset() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-ops-owner@example.com");
        long assetId = createAsset(ownerAuth);

        String otherAuth = authHeaderFor("inv-iso-ops-other@example.com");
        String body = String.format(
                "{\"type\":\"BUY\",\"quantity\":10,\"unitPrice\":50.00,\"operationDate\":\"%s\"}", LocalDate.now());

        mockMvc.perform(post("/investments/assets/" + assetId + "/operations")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/investments/assets/" + assetId + "/operations").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void sellExceedingCurrentQuantityIsRejected() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-sell-owner@example.com");
        long assetId = createAsset(ownerAuth);
        createOperation(ownerAuth, assetId);

        String body = String.format(
                "{\"type\":\"SELL\",\"quantity\":999,\"unitPrice\":50.00,\"operationDate\":\"%s\"}", LocalDate.now());
        mockMvc.perform(post("/investments/assets/" + assetId + "/operations")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnerCannotRecordValuationForAnotherUsersAsset() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-val-owner@example.com");
        long assetId = createAsset(ownerAuth);

        String otherAuth = authHeaderFor("inv-iso-val-other@example.com");
        String body = String.format("{\"valueDate\":\"%s\",\"totalValue\":1000.00}", LocalDate.now());

        mockMvc.perform(post("/investments/assets/" + assetId + "/valuations")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordingASecondValuationPreservesTheEarlierOneAsHistory() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-val-history@example.com");
        long assetId = createAsset(ownerAuth);

        mockMvc.perform(post("/investments/assets/" + assetId + "/valuations")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"valueDate\":\"%s\",\"totalValue\":1000.00}", LocalDate.now().minusDays(10))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/investments/assets/" + assetId + "/valuations")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"valueDate\":\"%s\",\"totalValue\":1200.00}", LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/investments/assets/" + assetId + "/valuations").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));

        mockMvc.perform(get("/investments/assets/" + assetId).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue", is(1200.0)));
    }

    @Test
    void operationsNeverAffectAnyAccountBalance() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-no-balance@example.com");
        long accountId = objectMapper.readTree(mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"XP\",\"icon\":\"wallet\",\"color\":\"#000\",\"type\":\"BROKERAGE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        long assetId = objectMapper.readTree(mockMvc.perform(post("/investments/assets")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"name\":\"PETR4\",\"ticker\":\"PETR4\",\"type\":\"STOCK\",\"accountId\":%d}", accountId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        createOperation(ownerAuth, assetId);

        mockMvc.perform(get("/accounts/" + accountId).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0)));
    }

    @Test
    void portfolioEndpointsAreScopedToTheCallersOwnAssets() throws Exception {
        String ownerAuth = authHeaderFor("inv-iso-portfolio-owner@example.com");
        long assetId = createAsset(ownerAuth);
        createOperation(ownerAuth, assetId);

        String otherAuth = authHeaderFor("inv-iso-portfolio-other@example.com");
        mockMvc.perform(get("/investments/portfolio/summary").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested", is(0)));

        mockMvc.perform(get("/investments/portfolio/summary").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested", is(500.0)));
    }
}
