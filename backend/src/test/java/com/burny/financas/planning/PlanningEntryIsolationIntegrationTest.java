package com.burny.financas.planning;

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
import java.time.YearMonth;
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
class PlanningEntryIsolationIntegrationTest {

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

    private long createPlanningEntry(String authHeader, long accountId) throws Exception {
        String body = String.format(
                "{\"type\":\"PAYABLE\",\"accountId\":%d,\"amount\":100.00,\"description\":\"Conta de luz\",\"dueDate\":\"%s\"}",
                accountId, LocalDate.now().plusDays(5));
        String response = mockMvc.perform(post("/planning-entries")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void creatingEntryAgainstAccountOwnedByAnotherUserIsRejected() throws Exception {
        String ownerAuth = authHeaderFor("planning-iso-owner@example.com");
        long accountId = createAccount(ownerAuth);

        String otherAuth = authHeaderFor("planning-iso-other@example.com");
        String body = String.format(
                "{\"type\":\"PAYABLE\",\"accountId\":%d,\"amount\":100.00,\"description\":\"x\",\"dueDate\":\"%s\"}",
                accountId, LocalDate.now().plusDays(5));

        mockMvc.perform(post("/planning-entries")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersEntry() throws Exception {
        String ownerAuth = authHeaderFor("planning-iso-get-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long entryId = createPlanningEntry(ownerAuth, accountId);

        String otherAuth = authHeaderFor("planning-iso-get-other@example.com");
        mockMvc.perform(get("/planning-entries/" + entryId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnEntries() throws Exception {
        String ownerAuth = authHeaderFor("planning-iso-list-owner@example.com");
        long accountId = createAccount(ownerAuth);
        createPlanningEntry(ownerAuth, accountId);

        String otherAuth = authHeaderFor("planning-iso-list-other@example.com");
        mockMvc.perform(get("/planning-entries").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        mockMvc.perform(get("/planning-entries").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void nonOwnerCannotUpdateDeleteSettleOrUndoAnotherUsersEntry() throws Exception {
        String ownerAuth = authHeaderFor("planning-iso-actions-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long entryId = createPlanningEntry(ownerAuth, accountId);

        String otherAuth = authHeaderFor("planning-iso-actions-other@example.com");
        String updateBody = String.format(
                "{\"type\":\"PAYABLE\",\"accountId\":%d,\"amount\":50.00,\"description\":\"y\",\"dueDate\":\"%s\"}",
                accountId, LocalDate.now().plusDays(10));

        mockMvc.perform(put("/planning-entries/" + entryId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/planning-entries/" + entryId + "/settle").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/planning-entries/" + entryId + "/undo-settlement").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/planning-entries/" + entryId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void calendarOnlyReturnsCallersOwnEntries() throws Exception {
        String ownerAuth = authHeaderFor("planning-iso-cal-owner@example.com");
        long accountId = createAccount(ownerAuth);
        createPlanningEntry(ownerAuth, accountId);

        String otherAuth = authHeaderFor("planning-iso-cal-other@example.com");
        String month = YearMonth.now().plusMonths(1).toString();
        mockMvc.perform(get("/planning-entries/calendar").param("month", month).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void settlingEntryCreatesTransactionAndUpdatesAccountBalance() throws Exception {
        String ownerAuth = authHeaderFor("planning-iso-settle-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long entryId = createPlanningEntry(ownerAuth, accountId);

        mockMvc.perform(post("/planning-entries/" + entryId + "/settle").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SETTLED")))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        mockMvc.perform(get("/accounts/" + accountId).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(-100.0)));
    }
}
