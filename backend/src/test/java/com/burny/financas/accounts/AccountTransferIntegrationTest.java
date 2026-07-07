package com.burny.financas.accounts;

import static org.hamcrest.Matchers.is;
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
class AccountTransferIntegrationTest {

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

    private String transferBody(long sourceId, long destId, String amount) {
        return "{\"sourceAccountId\":" + sourceId + ",\"destinationAccountId\":" + destId + ",\"amount\":" + amount + "}";
    }

    @Test
    void successfulTransferMovesBalanceBetweenOwnAccounts() throws Exception {
        String auth = authHeaderFor("transfer-success@example.com");
        long source = createAccount(auth, "Origem", "CHECKING", null);
        long dest = createAccount(auth, "Destino", "CHECKING", null);
        seedBalance(source, "500.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(source, dest, "200")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(200)));

        mockMvc.perform(get("/accounts/" + source).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(300.0)));
        mockMvc.perform(get("/accounts/" + dest).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(200)));
    }

    @Test
    void transferUpToExactBalanceLeavesSourceAtZero() throws Exception {
        String auth = authHeaderFor("transfer-exact@example.com");
        long source = createAccount(auth, "Origem", "CHECKING", null);
        long dest = createAccount(auth, "Destino", "CHECKING", null);
        seedBalance(source, "150.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(source, dest, "150")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/" + source).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(0.0)));
    }

    @Test
    void cannotTransferFromAnotherUsersAccount() throws Exception {
        String ownerAuth = authHeaderFor("transfer-source-owner@example.com");
        long theirSource = createAccount(ownerAuth, "Origem", "CHECKING", null);
        seedBalance(theirSource, "500.00");

        String attackerAuth = authHeaderFor("transfer-source-attacker@example.com");
        long attackerDest = createAccount(attackerAuth, "Destino", "CHECKING", null);

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, attackerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(theirSource, attackerDest, "100")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/accounts/" + theirSource).header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(jsonPath("$.balance", is(500.0)));
    }

    @Test
    void cannotTransferToAnotherUsersAccount() throws Exception {
        String auth = authHeaderFor("transfer-dest-owner@example.com");
        long mySource = createAccount(auth, "Origem", "CHECKING", null);
        seedBalance(mySource, "500.00");

        String otherAuth = authHeaderFor("transfer-dest-other@example.com");
        long theirAccount = createAccount(otherAuth, "Alheia", "CHECKING", null);

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(mySource, theirAccount, "100")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/accounts/" + mySource).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(500.0)));
    }

    @Test
    void nonPositiveAmountRejectedWith400() throws Exception {
        String auth = authHeaderFor("transfer-non-positive@example.com");
        long source = createAccount(auth, "Origem", "CHECKING", null);
        long dest = createAccount(auth, "Destino", "CHECKING", null);
        seedBalance(source, "500.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(source, dest, "0")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(source, dest, "-50")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferExceedingSourceBalanceRejectedWith422() throws Exception {
        String auth = authHeaderFor("transfer-insufficient@example.com");
        long source = createAccount(auth, "Origem", "CHECKING", null);
        long dest = createAccount(auth, "Destino", "CHECKING", null);
        seedBalance(source, "50.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(source, dest, "100")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/accounts/" + source).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(50.0)));
        mockMvc.perform(get("/accounts/" + dest).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(0)));
    }

    @Test
    void creditCardAsSourceRejectedWith422() throws Exception {
        String auth = authHeaderFor("transfer-cc-source@example.com");
        long card = createAccount(auth, "Cartao", "CREDIT_CARD", "1000");
        long dest = createAccount(auth, "Destino", "CHECKING", null);

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(card, dest, "100")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void paymentToCreditCardReducesCurrentInvoice() throws Exception {
        String auth = authHeaderFor("transfer-cc-payment@example.com");
        long checking = createAccount(auth, "Corrente", "CHECKING", null);
        long card = createAccount(auth, "Cartao", "CREDIT_CARD", "1000");
        seedBalance(checking, "500.00");
        seedInvoice(card, "300.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(checking, card, "200")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/" + card).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.currentInvoice", is(100.0)));
    }

    @Test
    void overpaymentToCreditCardFloorsInvoiceAtZero() throws Exception {
        String auth = authHeaderFor("transfer-cc-overpay@example.com");
        long checking = createAccount(auth, "Corrente", "CHECKING", null);
        long card = createAccount(auth, "Cartao", "CREDIT_CARD", "1000");
        seedBalance(checking, "500.00");
        seedInvoice(card, "100.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(checking, card, "300")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/" + card).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.currentInvoice", is(0)));
    }

    @Test
    void failedTransferLeavesBothBalancesUnchanged() throws Exception {
        // The insufficient-balance rejection is the reachable way to exercise "fails after
        // validation begins, no partial effect" without reaching into transaction internals.
        String auth = authHeaderFor("transfer-atomicity@example.com");
        long source = createAccount(auth, "Origem", "CHECKING", null);
        long dest = createAccount(auth, "Destino", "CHECKING", null);
        seedBalance(source, "10.00");
        seedBalance(dest, "10.00");

        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(source, dest, "9999")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/accounts/" + source).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(10.0)));
        mockMvc.perform(get("/accounts/" + dest).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.balance", is(10.0)));
    }
}
