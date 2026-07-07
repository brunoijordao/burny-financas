package com.burny.financas.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionRecurrence;
import com.burny.financas.transactions.repository.TransactionRecurrenceRepository;
import com.burny.financas.transactions.repository.TransactionRepository;
import com.burny.financas.transactions.service.TransactionRecurrenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
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
class TransactionRecurrenceIntegrationTest {

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

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionRecurrenceRepository transactionRecurrenceRepository;

    @Autowired
    private TransactionRecurrenceService transactionRecurrenceService;

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

    private long createRecurringTransaction(String authHeader, long accountId, String startDate, String endDate) throws Exception {
        String body = "{"
                + "\"description\":\"Aluguel\",\"amount\":100.00,\"type\":\"EXPENSE\","
                + "\"transactionDate\":\"" + startDate + "\",\"accountId\":" + accountId + ","
                + "\"recurring\":true,\"frequency\":\"MONTHLY\",\"startDate\":\"" + startDate + "\""
                + (endDate != null ? ",\"endDate\":\"" + endDate + "\"" : "")
                + "}";
        String response = mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurrenceId", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("recurrenceId").asLong();
    }

    @Test
    void creatingRecurringTransactionPersistsFirstOccurrenceAndRecurrence() throws Exception {
        String auth = authHeaderFor("tx-recur-create@example.com");
        long accountId = createAccount(auth);

        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", null);

        TransactionRecurrence recurrence = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(recurrence.isActive()).isTrue();
        assertThat(recurrence.getNextOccurrenceDate()).isEqualTo(LocalDate.of(2026, 2, 15));

        List<Transaction> occurrences = transactionRepository
                .findTopByRecurrenceIdOrderByTransactionDateDescIdDesc(recurrenceId)
                .map(List::of)
                .orElse(List.of());
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.get(0).getTransactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void recurringTransactionRequiresFrequency() throws Exception {
        String auth = authHeaderFor("tx-recur-no-frequency@example.com");
        long accountId = createAccount(auth);

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Aluguel\",\"amount\":100.00,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId
                                + ",\"recurring\":true,\"startDate\":\"2026-01-15\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduledGenerationCreatesDueOccurrenceAndAdvancesNextDate() throws Exception {
        String auth = authHeaderFor("tx-recur-generate@example.com");
        long accountId = createAccount(auth);
        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", null);

        // Force the recurrence to be due "today" for a deterministic test.
        TransactionRecurrence recurrence = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        LocalDate dueDate = LocalDate.now();
        recurrence.setNextOccurrenceDate(dueDate);
        transactionRecurrenceRepository.save(recurrence);

        transactionRecurrenceService.generateDueOccurrences();

        TransactionRecurrence updated = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(updated.getNextOccurrenceDate()).isEqualTo(dueDate.plusMonths(1));

        Transaction latest = transactionRepository
                .findTopByRecurrenceIdOrderByTransactionDateDescIdDesc(recurrenceId).orElseThrow();
        assertThat(latest.getTransactionDate()).isEqualTo(dueDate);
        assertThat(latest.getDescription()).isEqualTo("Aluguel");
    }

    @Test
    void generationStopsAfterEndDate() throws Exception {
        String auth = authHeaderFor("tx-recur-end-date@example.com");
        long accountId = createAccount(auth);
        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", "2026-01-20");

        TransactionRecurrence recurrence = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        // next_occurrence_date already past end_date (2026-02-15 > 2026-01-20), so nothing should generate.
        transactionRecurrenceService.generateDueOccurrences();

        long occurrenceCountAfter = countOccurrences(recurrenceId);
        assertThat(occurrenceCountAfter).isEqualTo(1);

        TransactionRecurrence unchanged = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(unchanged.getNextOccurrenceDate()).isEqualTo(recurrence.getNextOccurrenceDate());
    }

    @Test
    void backlogOfMultipleMissedOccurrencesAllGeneratedInOneRun() throws Exception {
        String auth = authHeaderFor("tx-recur-backlog@example.com");
        long accountId = createAccount(auth);
        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", null);

        // Simulate the server having been down: next occurrence is 3 months in the past.
        TransactionRecurrence recurrence = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        LocalDate longOverdue = LocalDate.now().minusMonths(3);
        recurrence.setNextOccurrenceDate(longOverdue);
        transactionRecurrenceRepository.save(recurrence);

        transactionRecurrenceService.generateDueOccurrences();

        // 1 (manual) + at least 3 backlogged occurrences generated to catch up to today.
        long occurrenceCount = countOccurrences(recurrenceId);
        assertThat(occurrenceCount).isGreaterThanOrEqualTo(4);

        TransactionRecurrence updated = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(updated.getNextOccurrenceDate()).isAfter(LocalDate.now());
    }

    @Test
    void editingGeneratedOccurrenceDoesNotAffectRecurrenceOrOtherOccurrences() throws Exception {
        String auth = authHeaderFor("tx-recur-edit-occurrence@example.com");
        long accountId = createAccount(auth);
        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", null);

        TransactionRecurrence recurrence = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        recurrence.setNextOccurrenceDate(LocalDate.now());
        transactionRecurrenceRepository.save(recurrence);
        transactionRecurrenceService.generateDueOccurrences();

        Transaction generated = transactionRepository
                .findTopByRecurrenceIdOrderByTransactionDateDescIdDesc(recurrenceId).orElseThrow();

        mockMvc.perform(put("/transactions/" + generated.getId())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Aluguel editado\",\"amount\":150.00,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"" + generated.getTransactionDate() + "\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isOk());

        TransactionRecurrence unchanged = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(unchanged.isActive()).isTrue();

        // Recurrence still generates correctly afterward.
        unchanged.setNextOccurrenceDate(LocalDate.now());
        transactionRecurrenceRepository.save(unchanged);
        transactionRecurrenceService.generateDueOccurrences();
        assertThat(countOccurrences(recurrenceId)).isEqualTo(3);
    }

    @Test
    void deletingGeneratedOccurrenceDoesNotStopRecurrence() throws Exception {
        String auth = authHeaderFor("tx-recur-delete-occurrence@example.com");
        long accountId = createAccount(auth);
        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", null);

        TransactionRecurrence recurrence = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        recurrence.setNextOccurrenceDate(LocalDate.now());
        transactionRecurrenceRepository.save(recurrence);
        transactionRecurrenceService.generateDueOccurrences();

        Transaction generated = transactionRepository
                .findTopByRecurrenceIdOrderByTransactionDateDescIdDesc(recurrenceId).orElseThrow();

        mockMvc.perform(delete("/transactions/" + generated.getId()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        TransactionRecurrence stillActive = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(stillActive.isActive()).isTrue();
    }

    @Test
    void cancellingRecurrenceStopsFutureGenerationWithoutAffectingPastOccurrences() throws Exception {
        String auth = authHeaderFor("tx-recur-cancel@example.com");
        long accountId = createAccount(auth);
        long recurrenceId = createRecurringTransaction(auth, accountId, "2026-01-15", null);

        mockMvc.perform(delete("/transactions/recurrences/" + recurrenceId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        TransactionRecurrence cancelled = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(cancelled.isActive()).isFalse();

        cancelled.setNextOccurrenceDate(LocalDate.now());
        transactionRecurrenceRepository.save(cancelled);
        transactionRecurrenceService.generateDueOccurrences();

        assertThat(countOccurrences(recurrenceId)).isEqualTo(1);

        // The original occurrence remains intact.
        mockMvc.perform(get("/transactions").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void cannotCancelAnotherUsersRecurrence() throws Exception {
        String ownerAuth = authHeaderFor("tx-recur-cancel-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long recurrenceId = createRecurringTransaction(ownerAuth, accountId, "2026-01-15", null);

        String otherAuth = authHeaderFor("tx-recur-cancel-other@example.com");

        mockMvc.perform(delete("/transactions/recurrences/" + recurrenceId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        TransactionRecurrence stillActive = transactionRecurrenceRepository.findById(recurrenceId).orElseThrow();
        assertThat(stillActive.isActive()).isTrue();
    }

    private long countOccurrences(long recurrenceId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getRecurrence() != null && t.getRecurrence().getId().equals(recurrenceId))
                .count();
    }
}
