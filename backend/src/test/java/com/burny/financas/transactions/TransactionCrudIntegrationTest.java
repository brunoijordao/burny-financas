package com.burny.financas.transactions;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class TransactionCrudIntegrationTest {

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
    private AccountRepository accountRepository;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private long createAccount(String authHeader, String name, String type, String creditLimit) throws Exception {
        String body = "{"
                + "\"name\":\"" + name + "\","
                + "\"icon\":\"wallet\","
                + "\"color\":\"#123456\","
                + "\"type\":\"" + type + "\""
                + (creditLimit != null ? ",\"creditLimit\":" + creditLimit : "")
                + "}";
        String response = mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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

    private void seedBalance(long accountId, String amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setBalance(new BigDecimal(amount));
        accountRepository.save(account);
    }

    private void seedInvoice(long accountId, String amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setCurrentInvoice(new BigDecimal(amount));
        accountRepository.save(account);
    }

    private BigDecimal balanceOf(long accountId) {
        return accountRepository.findById(accountId).orElseThrow().getBalance();
    }

    private BigDecimal invoiceOf(long accountId) {
        return accountRepository.findById(accountId).orElseThrow().getCurrentInvoice();
    }

    private long createTransaction(
            String authHeader, String type, String amount, long accountId, Long categoryId, String description
    ) throws Exception {
        String body = "{"
                + "\"description\":\"" + description + "\","
                + "\"amount\":" + amount + ","
                + "\"type\":\"" + type + "\","
                + "\"transactionDate\":\"2026-01-15\","
                + "\"accountId\":" + accountId
                + (categoryId != null ? ",\"categoryId\":" + categoryId : "")
                + "}";
        String response = mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void expenseDebitsCheckingAccount() throws Exception {
        String auth = authHeaderFor("tx-expense-checking@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        seedBalance(accountId, "100.00");

        createTransaction(auth, "EXPENSE", "30.00", accountId, null, "Mercado");

        assertBalance(accountId, "70.00");
    }

    @Test
    void incomeCreditsSavingsAccount() throws Exception {
        String auth = authHeaderFor("tx-income-savings@example.com");
        long accountId = createAccount(auth, "Poupanca", "SAVINGS", null);
        seedBalance(accountId, "100.00");

        createTransaction(auth, "INCOME", "50.00", accountId, null, "Salario");

        assertBalance(accountId, "150.00");
    }

    @Test
    void expenseIncreasesCreditCardInvoice() throws Exception {
        String auth = authHeaderFor("tx-expense-cc@example.com");
        long accountId = createAccount(auth, "Cartao", "CREDIT_CARD", "5000");
        seedInvoice(accountId, "50.00");

        createTransaction(auth, "EXPENSE", "20.00", accountId, null, "Compra");

        assertInvoice(accountId, "70.00");
    }

    @Test
    void incomeReducesCreditCardInvoiceFlooredAtZero() throws Exception {
        String auth = authHeaderFor("tx-income-cc@example.com");
        long accountId = createAccount(auth, "Cartao", "CREDIT_CARD", "5000");
        seedInvoice(accountId, "50.00");

        createTransaction(auth, "INCOME", "200.00", accountId, null, "Estorno");

        assertInvoice(accountId, "0.00");
    }

    @Test
    void expenseAllowsBalanceToGoNegative() throws Exception {
        String auth = authHeaderFor("tx-negative-balance@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        seedBalance(accountId, "10.00");

        createTransaction(auth, "EXPENSE", "50.00", accountId, null, "Despesa grande");

        assertBalance(accountId, "-40.00");
    }

    @Test
    void amountMustBePositive() throws Exception {
        String auth = authHeaderFor("tx-non-positive@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\",\"amount\":0,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isBadRequest());

        assertBalance(accountId, "0.00");
    }

    @Test
    void accountMustBelongToCaller() throws Exception {
        String ownerAuth = authHeaderFor("tx-account-owner@example.com");
        long accountId = createAccount(ownerAuth, "Conta", "CHECKING", null);

        String otherAuth = authHeaderFor("tx-account-other@example.com");

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\",\"amount\":10,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoryMustBelongToCallerWhenProvided() throws Exception {
        String ownerAuth = authHeaderFor("tx-category-owner@example.com");
        long categoryId = createCategory(ownerAuth, "Categoria");

        String otherAuth = authHeaderFor("tx-category-other@example.com");
        long accountId = createAccount(otherAuth, "Conta", "CHECKING", null);

        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\",\"amount\":10,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId
                                + ",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanRetrieveTheirTransaction() throws Exception {
        String auth = authHeaderFor("tx-owner-get@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        long txId = createTransaction(auth, "EXPENSE", "10.00", accountId, null, "X");

        mockMvc.perform(get("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) txId)));
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersTransaction() throws Exception {
        String ownerAuth = authHeaderFor("tx-isolation-owner@example.com");
        long accountId = createAccount(ownerAuth, "Conta", "CHECKING", null);
        long txId = createTransaction(ownerAuth, "EXPENSE", "10.00", accountId, null, "X");

        String otherAuth = authHeaderFor("tx-isolation-other@example.com");

        mockMvc.perform(get("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnTransactions() throws Exception {
        String auth = authHeaderFor("tx-list-own@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        createTransaction(auth, "EXPENSE", "10.00", accountId, null, "Minha");

        String otherAuth = authHeaderFor("tx-list-other@example.com");
        long otherAccountId = createAccount(otherAuth, "Conta", "CHECKING", null);
        createTransaction(otherAuth, "EXPENSE", "10.00", otherAccountId, null, "Alheia");

        mockMvc.perform(get("/transactions").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].description", is("Minha")));
    }

    @Test
    void listingFiltersByAccountCategoryTypeAndDateRange() throws Exception {
        String auth = authHeaderFor("tx-list-filters@example.com");
        long accountA = createAccount(auth, "Conta A", "CHECKING", null);
        long accountB = createAccount(auth, "Conta B", "CHECKING", null);
        long categoryId = createCategory(auth, "Categoria");

        createTransaction(auth, "EXPENSE", "10.00", accountA, categoryId, "A-expense-categorized");
        createTransaction(auth, "INCOME", "20.00", accountA, null, "A-income");
        createTransaction(auth, "EXPENSE", "30.00", accountB, null, "B-expense");

        mockMvc.perform(get("/transactions?accountId=" + accountA).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)));

        mockMvc.perform(get("/transactions?categoryId=" + categoryId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].description", is("A-expense-categorized")));

        mockMvc.perform(get("/transactions?type=INCOME").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].description", is("A-income")));

        mockMvc.perform(get("/transactions?startDate=2026-01-15&endDate=2026-01-15")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(3)));

        mockMvc.perform(get("/transactions?startDate=2026-02-01&endDate=2026-02-28")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(0)));
    }

    @Test
    void listingIsPaginated() throws Exception {
        String auth = authHeaderFor("tx-list-paginated@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        for (int i = 0; i < 3; i++) {
            createTransaction(auth, "EXPENSE", "10.00", accountId, null, "Tx" + i);
        }

        mockMvc.perform(get("/transactions?page=0&size=2").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void editingAmountReversesAndReappliesOnSameAccount() throws Exception {
        String auth = authHeaderFor("tx-edit-amount@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        seedBalance(accountId, "100.00");
        long txId = createTransaction(auth, "EXPENSE", "20.00", accountId, null, "Original");
        assertBalance(accountId, "80.00");

        mockMvc.perform(put("/transactions/" + txId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Editada\",\"amount\":50.00,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-16\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Editada")));

        assertBalance(accountId, "50.00");
    }

    @Test
    void editingAccountMovesBalanceEffect() throws Exception {
        String auth = authHeaderFor("tx-edit-account@example.com");
        long accountA = createAccount(auth, "Conta A", "CHECKING", null);
        long accountB = createAccount(auth, "Conta B", "CHECKING", null);
        seedBalance(accountA, "100.00");
        seedBalance(accountB, "100.00");
        long txId = createTransaction(auth, "EXPENSE", "30.00", accountA, null, "Original");
        assertBalance(accountA, "70.00");

        mockMvc.perform(put("/transactions/" + txId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Movida\",\"amount\":30.00,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-16\",\"accountId\":" + accountB + "}"))
                .andExpect(status().isOk());

        assertBalance(accountA, "100.00");
        assertBalance(accountB, "70.00");
    }

    @Test
    void editingTypeFlipsBalanceEffect() throws Exception {
        String auth = authHeaderFor("tx-edit-type@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        seedBalance(accountId, "100.00");
        long txId = createTransaction(auth, "EXPENSE", "20.00", accountId, null, "Original");
        assertBalance(accountId, "80.00");

        mockMvc.perform(put("/transactions/" + txId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Agora receita\",\"amount\":20.00,\"type\":\"INCOME\","
                                + "\"transactionDate\":\"2026-01-16\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isOk());

        assertBalance(accountId, "120.00");
    }

    @Test
    void cannotEditAnotherUsersTransaction() throws Exception {
        String ownerAuth = authHeaderFor("tx-edit-owner@example.com");
        long accountId = createAccount(ownerAuth, "Conta", "CHECKING", null);
        long txId = createTransaction(ownerAuth, "EXPENSE", "20.00", accountId, null, "Original");

        String otherAuth = authHeaderFor("tx-edit-other@example.com");

        mockMvc.perform(put("/transactions/" + txId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Hacked\",\"amount\":1.00,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-16\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isNotFound());

        assertBalance(accountId, "-20.00");
    }

    @Test
    void newAccountMustBelongToCaller() throws Exception {
        String ownerAuth = authHeaderFor("tx-edit-newaccount-owner@example.com");
        long ownAccountId = createAccount(ownerAuth, "Conta", "CHECKING", null);
        long txId = createTransaction(ownerAuth, "EXPENSE", "20.00", ownAccountId, null, "Original");

        String otherAuth = authHeaderFor("tx-edit-newaccount-other@example.com");
        long otherAccountId = createAccount(otherAuth, "Conta Alheia", "CHECKING", null);

        mockMvc.perform(put("/transactions/" + txId)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\",\"amount\":20.00,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-16\",\"accountId\":" + otherAccountId + "}"))
                .andExpect(status().isNotFound());

        assertBalance(ownAccountId, "-20.00");
    }

    @Test
    void deletingExpenseRestoresBalance() throws Exception {
        String auth = authHeaderFor("tx-delete-expense@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        seedBalance(accountId, "100.00");
        long txId = createTransaction(auth, "EXPENSE", "30.00", accountId, null, "X");
        assertBalance(accountId, "70.00");

        mockMvc.perform(delete("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        assertBalance(accountId, "100.00");
    }

    @Test
    void deletingIncomeReversesCredit() throws Exception {
        String auth = authHeaderFor("tx-delete-income@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        seedBalance(accountId, "100.00");
        long txId = createTransaction(auth, "INCOME", "30.00", accountId, null, "X");
        assertBalance(accountId, "130.00");

        mockMvc.perform(delete("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        assertBalance(accountId, "100.00");
    }

    @Test
    void cannotDeleteAnotherUsersTransaction() throws Exception {
        String ownerAuth = authHeaderFor("tx-delete-owner@example.com");
        long accountId = createAccount(ownerAuth, "Conta", "CHECKING", null);
        long txId = createTransaction(ownerAuth, "EXPENSE", "20.00", accountId, null, "X");

        String otherAuth = authHeaderFor("tx-delete-other@example.com");

        mockMvc.perform(delete("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        assertBalance(accountId, "-20.00");
    }

    @Test
    void inactiveTransactionsExcludedFromListing() throws Exception {
        String auth = authHeaderFor("tx-list-excludes-inactive@example.com");
        long accountId = createAccount(auth, "Conta", "CHECKING", null);
        long txId = createTransaction(auth, "EXPENSE", "10.00", accountId, null, "X");

        mockMvc.perform(delete("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transactions").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(0)));
    }

    private void assertBalance(long accountId, String expected) {
        org.assertj.core.api.Assertions.assertThat(balanceOf(accountId)).isEqualByComparingTo(expected);
    }

    private void assertInvoice(long accountId, String expected) {
        org.assertj.core.api.Assertions.assertThat(invoiceOf(accountId)).isEqualByComparingTo(expected);
    }
}
