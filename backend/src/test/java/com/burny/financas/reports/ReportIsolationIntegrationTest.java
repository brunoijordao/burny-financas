package com.burny.financas.reports;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ReportIsolationIntegrationTest {

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

    private void createTransaction(String authHeader, long accountId, String type, String amount) throws Exception {
        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Compra\",\"amount\":" + amount + ",\"type\":\"" + type + "\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void statementOnlyReturnsCallersOwnTransactions() throws Exception {
        String ownerAuth = authHeaderFor("rpt-iso-statement-owner@example.com");
        long accountId = createAccount(ownerAuth);
        createTransaction(ownerAuth, accountId, "EXPENSE", "150.00");

        String otherAuth = authHeaderFor("rpt-iso-statement-other@example.com");

        mockMvc.perform(get("/reports/statement")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        mockMvc.perform(get("/reports/statement")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].amount", is(150.0)));
    }

    @Test
    void statementRejectsInvertedDateRangeWith400() throws Exception {
        String auth = authHeaderFor("rpt-iso-statement-inverted@example.com");

        mockMvc.perform(get("/reports/statement")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("startDate", "2026-02-01").param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void spendingByCategoryOnlyReflectsCallersOwnExpenses() throws Exception {
        String ownerAuth = authHeaderFor("rpt-iso-spending-owner@example.com");
        long accountId = createAccount(ownerAuth);
        createTransaction(ownerAuth, accountId, "EXPENSE", "200.00");

        String otherAuth = authHeaderFor("rpt-iso-spending-other@example.com");

        mockMvc.perform(get("/reports/spending-by-category")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        mockMvc.perform(get("/reports/spending-by-category")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].categoryName", is("Sem categoria")))
                .andExpect(jsonPath("$[0].percentage", is(100.0)));
    }

    @Test
    void netWorthEvolutionOnlyReflectsCallersOwnAccounts() throws Exception {
        String ownerAuth = authHeaderFor("rpt-iso-networth-owner@example.com");
        long accountId = createAccount(ownerAuth);
        createTransaction(ownerAuth, accountId, "INCOME", "500.00");

        String otherAuth = authHeaderFor("rpt-iso-networth-other@example.com");

        mockMvc.perform(get("/reports/net-worth-evolution").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentConsolidatedAccountBalance", is(0)));

        mockMvc.perform(get("/reports/net-worth-evolution").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentConsolidatedAccountBalance", is(500.0)));
    }

    @Test
    void exportStatementAsPdfReturnsCorrectContentTypeAndDisposition() throws Exception {
        String auth = authHeaderFor("rpt-export-statement-pdf@example.com");

        mockMvc.perform(get("/reports/statement/export")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31").param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment; filename=\"extrato_")));
    }

    @Test
    void exportStatementAsXlsxReturnsCorrectContentTypeAndDisposition() throws Exception {
        String auth = authHeaderFor("rpt-export-statement-xlsx@example.com");

        mockMvc.perform(get("/reports/statement/export")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment; filename=\"extrato_")));
    }

    @Test
    void exportSpendingByCategoryAsPdfAndXlsxReturnCorrectContentTypes() throws Exception {
        String auth = authHeaderFor("rpt-export-spending@example.com");

        mockMvc.perform(get("/reports/spending-by-category/export")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31").param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"));

        mockMvc.perform(get("/reports/spending-by-category/export")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportRejectsUnsupportedFormatWith400() throws Exception {
        String auth = authHeaderFor("rpt-export-bad-format@example.com");

        mockMvc.perform(get("/reports/statement/export")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("startDate", "2026-01-01").param("endDate", "2026-01-31").param("format", "docx"))
                .andExpect(status().isBadRequest());
    }
}
