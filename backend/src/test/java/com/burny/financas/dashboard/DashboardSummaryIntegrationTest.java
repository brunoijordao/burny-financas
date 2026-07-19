package com.burny.financas.dashboard;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class DashboardSummaryIntegrationTest {

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
                        .content("{\"name\":\"Conta\",\"icon\":\"wallet\",\"color\":\"#123456\",\"type\":\"CHECKING\"}"))
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

    private void createTransaction(
            String authHeader, String type, String amount, long accountId, Long categoryId, String date
    ) throws Exception {
        String body = "{"
                + "\"description\":\"X\","
                + "\"amount\":" + amount + ","
                + "\"type\":\"" + type + "\","
                + "\"transactionDate\":\"" + date + "\","
                + "\"accountId\":" + accountId
                + (categoryId != null ? ",\"categoryId\":" + categoryId : "")
                + "}";
        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void defaultReferenceDateUsesCurrentMonth() throws Exception {
        String auth = authHeaderFor("dash-default-month@example.com");

        mockMvc.perform(get("/dashboard/summary").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is(YearMonth.now().toString())));
    }

    @Test
    void explicitReferenceDateScopesMonthAndTrend() throws Exception {
        String auth = authHeaderFor("dash-explicit-month@example.com");
        long accountId = createAccount(auth);
        createTransaction(auth, "INCOME", "500.00", accountId, null, "2026-02-10");
        createTransaction(auth, "INCOME", "999.00", accountId, null, "2026-01-10");

        mockMvc.perform(get("/dashboard/summary?referenceDate=2026-02-15").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-02")))
                .andExpect(jsonPath("$.monthIncome", is(500.0)))
                .andExpect(jsonPath("$.monthlyTrend.length()", is(6)))
                .andExpect(jsonPath("$.monthlyTrend[0].month", is("2025-09")))
                .andExpect(jsonPath("$.monthlyTrend[5].month", is("2026-02")));
    }

    @Test
    void monthTotalsIncludeTransactionsBeforeAndAfterReferenceDate() throws Exception {
        String auth = authHeaderFor("dash-month-totals@example.com");
        long accountId = createAccount(auth);
        createTransaction(auth, "INCOME", "100.00", accountId, null, "2026-03-01");
        createTransaction(auth, "EXPENSE", "40.00", accountId, null, "2026-03-28");

        mockMvc.perform(get("/dashboard/summary?referenceDate=2026-03-15").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthIncome", is(100.0)))
                .andExpect(jsonPath("$.monthExpense", is(40.0)))
                .andExpect(jsonPath("$.monthNet", is(60.0)));
    }

    @Test
    void futureIncomeAndExpenseOnlyIncludeDatesAfterReferenceDate() throws Exception {
        String auth = authHeaderFor("dash-future-split@example.com");
        long accountId = createAccount(auth);
        createTransaction(auth, "INCOME", "200.00", accountId, null, "2026-04-05");
        createTransaction(auth, "INCOME", "50.00", accountId, null, "2026-04-20");
        createTransaction(auth, "EXPENSE", "30.00", accountId, null, "2026-04-25");

        mockMvc.perform(get("/dashboard/summary?referenceDate=2026-04-10").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futureIncome", is(50.0)))
                .andExpect(jsonPath("$.futureExpense", is(30.0)));
    }

    @Test
    void categoryBreakdownGroupsCategorizedAndUncategorizedExpensesAndExcludesIncome() throws Exception {
        String auth = authHeaderFor("dash-category-breakdown@example.com");
        long accountId = createAccount(auth);
        long categoryId = createCategory(auth, "Mercado");
        createTransaction(auth, "EXPENSE", "200.00", accountId, categoryId, "2026-05-05");
        createTransaction(auth, "EXPENSE", "150.00", accountId, categoryId, "2026-05-06");
        createTransaction(auth, "EXPENSE", "70.00", accountId, null, "2026-05-07");
        createTransaction(auth, "INCOME", "1000.00", accountId, null, "2026-05-08");

        mockMvc.perform(get("/dashboard/summary?referenceDate=2026-05-15").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryBreakdown.length()", is(2)))
                .andExpect(jsonPath("$.categoryBreakdown[0].categoryName", is("Mercado")))
                .andExpect(jsonPath("$.categoryBreakdown[0].total", is(350.0)))
                .andExpect(jsonPath("$.categoryBreakdown[1].categoryName", is("Sem categoria")))
                .andExpect(jsonPath("$.categoryBreakdown[1].total", is(70.0)));
    }

    @Test
    void monthlyTrendCoversSixChronologicalMonthsWithZeroFilledGaps() throws Exception {
        String auth = authHeaderFor("dash-trend-zero-fill@example.com");
        long accountId = createAccount(auth);
        createTransaction(auth, "INCOME", "300.00", accountId, null, "2026-06-10");

        mockMvc.perform(get("/dashboard/summary?referenceDate=2026-06-15").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTrend.length()", is(6)))
                .andExpect(jsonPath("$.monthlyTrend[0].month", is("2026-01")))
                .andExpect(jsonPath("$.monthlyTrend[0].income", is(0)))
                .andExpect(jsonPath("$.monthlyTrend[0].expense", is(0)))
                .andExpect(jsonPath("$.monthlyTrend[5].month", is("2026-06")))
                .andExpect(jsonPath("$.monthlyTrend[5].income", is(300.0)));
    }

    @Test
    void summaryIsIsolatedPerUser() throws Exception {
        String ownerAuth = authHeaderFor("dash-isolation-owner@example.com");
        long ownerAccountId = createAccount(ownerAuth);
        createTransaction(ownerAuth, "INCOME", "500.00", ownerAccountId, null, "2026-07-05");

        String otherAuth = authHeaderFor("dash-isolation-other@example.com");
        long otherAccountId = createAccount(otherAuth);
        createTransaction(otherAuth, "INCOME", "9999.00", otherAccountId, null, "2026-07-05");

        mockMvc.perform(get("/dashboard/summary?referenceDate=2026-07-15").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthIncome", is(500.0)));
    }
}
