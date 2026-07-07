package com.burny.financas.accounts;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AccountBalanceIntegrationTest {

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

    @Test
    void consolidatedBalanceSumsNonCreditCardAccounts() throws Exception {
        String auth = authHeaderFor("balance-sum@example.com");
        long checking = createAccount(auth, "Corrente", "CHECKING", null);
        long savings = createAccount(auth, "Poupanca", "SAVINGS", null);
        seedBalance(checking, "400.00");
        seedBalance(savings, "600.00");

        mockMvc.perform(get("/accounts/balance").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance", is(1000.0)));
    }

    @Test
    void consolidatedBalanceSubtractsCreditCardInvoices() throws Exception {
        String auth = authHeaderFor("balance-subtract@example.com");
        long checking = createAccount(auth, "Corrente", "CHECKING", null);
        long card = createAccount(auth, "Cartao", "CREDIT_CARD", "2000");
        seedBalance(checking, "1000.00");
        seedInvoice(card, "300.00");

        mockMvc.perform(get("/accounts/balance").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance", is(700.0)));
    }

    @Test
    void inactiveAccountsExcludedFromConsolidatedBalance() throws Exception {
        String auth = authHeaderFor("balance-inactive@example.com");
        long active = createAccount(auth, "Ativa", "CHECKING", null);
        long toDeactivate = createAccount(auth, "Inativa", "CHECKING", null);
        seedBalance(active, "500.00");
        seedBalance(toDeactivate, "10000.00");

        mockMvc.perform(delete("/accounts/" + toDeactivate).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/balance").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidatedBalance", is(500.0)));
    }

    @Test
    void individualBalanceReturnedForNonCreditCardAccount() throws Exception {
        String auth = authHeaderFor("individual-balance@example.com");
        long checking = createAccount(auth, "Corrente", "CHECKING", null);
        seedBalance(checking, "250.50");

        mockMvc.perform(get("/accounts/" + checking).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(250.50)));
    }

    @Test
    void individualBalanceReturnsInvoiceAndLimitForCreditCardAccount() throws Exception {
        String auth = authHeaderFor("individual-cc-balance@example.com");
        long card = createAccount(auth, "Cartao", "CREDIT_CARD", "3000");
        seedInvoice(card, "450.00");

        mockMvc.perform(get("/accounts/" + card).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentInvoice", is(450.00)))
                .andExpect(jsonPath("$.creditLimit", is(3000)))
                .andExpect(jsonPath("$.balance").doesNotExist());
    }
}
