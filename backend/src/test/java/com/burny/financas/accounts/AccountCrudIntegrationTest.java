package com.burny.financas.accounts;

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
class AccountCrudIntegrationTest {

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

    /** Seeds a balance directly via the repository, since there is no deposit endpoint in scope. */
    private void seedBalance(long accountId, String amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setBalance(new BigDecimal(amount));
        accountRepository.save(account);
    }

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

    @Test
    void successfulAccountCreationHasZeroBalance() throws Exception {
        String auth = authHeaderFor("account-create@example.com");

        mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nubank\",\"icon\":\"wallet\",\"color\":\"#820AD1\",\"type\":\"CHECKING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Nubank")))
                .andExpect(jsonPath("$.type", is("CHECKING")))
                .andExpect(jsonPath("$.balance", is(0)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void accountCreationWithInvalidTypeRejectedWith400() throws Exception {
        String auth = authHeaderFor("invalid-type@example.com");

        mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"icon\":\"wallet\",\"color\":\"#000\",\"type\":\"NOT_A_TYPE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditCardCreationWithoutLimitRejectedWith400() throws Exception {
        String auth = authHeaderFor("cc-no-limit@example.com");

        mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cartao\",\"icon\":\"card\",\"color\":\"#000\",\"type\":\"CREDIT_CARD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditCardCreationStartsWithZeroInvoiceAndReportsLimitInsteadOfBalance() throws Exception {
        String auth = authHeaderFor("cc-with-limit@example.com");

        mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cartao\",\"icon\":\"card\",\"color\":\"#000\",\"type\":\"CREDIT_CARD\",\"creditLimit\":5000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentInvoice", is(0)))
                .andExpect(jsonPath("$.creditLimit", is(5000)))
                .andExpect(jsonPath("$.balance").doesNotExist());
    }

    @Test
    void nonCreditCardAccountIgnoresSuppliedCreditLimit() throws Exception {
        String auth = authHeaderFor("ignore-limit@example.com");

        mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Poupanca\",\"icon\":\"piggy\",\"color\":\"#000\",\"type\":\"SAVINGS\",\"creditLimit\":999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditLimit").doesNotExist())
                .andExpect(jsonPath("$.currentInvoice").doesNotExist());
    }

    @Test
    void ownerCanRetrieveTheirAccount() throws Exception {
        String auth = authHeaderFor("owner-get@example.com");
        long id = createAccount(auth, "Conta", "CHECKING", null);

        mockMvc.perform(get("/accounts/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)));
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersAccount() throws Exception {
        String ownerAuth = authHeaderFor("owner-isolation@example.com");
        long id = createAccount(ownerAuth, "Conta", "CHECKING", null);

        String otherAuth = authHeaderFor("other-isolation@example.com");

        mockMvc.perform(get("/accounts/" + id).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnAccounts() throws Exception {
        String auth = authHeaderFor("list-own@example.com");
        createAccount(auth, "Minha Conta", "CHECKING", null);

        String otherAuth = authHeaderFor("list-other@example.com");
        createAccount(otherAuth, "Conta Alheia", "CHECKING", null);

        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("Minha Conta")));
    }

    @Test
    void successfulEditUpdatesNameIconAndColor() throws Exception {
        String auth = authHeaderFor("edit-success@example.com");
        long id = createAccount(auth, "Old Name", "CHECKING", null);

        mockMvc.perform(put("/accounts/" + id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"icon\":\"new-icon\",\"color\":\"#FFFFFF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.icon", is("new-icon")));
    }

    @Test
    void cannotEditAnotherUsersAccount() throws Exception {
        String ownerAuth = authHeaderFor("edit-owner@example.com");
        long id = createAccount(ownerAuth, "Conta", "CHECKING", null);

        String otherAuth = authHeaderFor("edit-other@example.com");

        mockMvc.perform(put("/accounts/" + id)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"icon\":\"x\",\"color\":\"#000\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void editRequestCannotChangeBalanceOrCurrentInvoice() throws Exception {
        String auth = authHeaderFor("edit-protected-fields@example.com");
        long id = createAccount(auth, "Conta", "CHECKING", null);

        // balance/currentInvoice aren't fields on UpdateAccountRequest at all, so sending them is a
        // no-op regardless of Jackson's unknown-property handling; assert they stay untouched.
        mockMvc.perform(put("/accounts/" + id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conta\",\"icon\":\"wallet\",\"color\":\"#000\",\"balance\":99999,\"currentInvoice\":99999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0)));
    }

    @Test
    void deletingAccountWithNoMovementHardDeletes() throws Exception {
        String auth = authHeaderFor("delete-hard@example.com");
        long id = createAccount(auth, "Conta", "CHECKING", null);

        mockMvc.perform(delete("/accounts/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Hard-deleted: not retrievable at all, even by id (unlike a soft-deleted account).
        mockMvc.perform(get("/accounts/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAccountWithLinkedTransferSoftDeletesInstead() throws Exception {
        String auth = authHeaderFor("delete-soft@example.com");
        long sourceId = createAccount(auth, "Origem", "CHECKING", null);
        long destId = createAccount(auth, "Destino", "CHECKING", null);
        seedBalance(sourceId, "100.00");

        // Transfer out of the source, linking it to a transfer record.
        mockMvc.perform(post("/accounts/transfers")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":" + sourceId + ",\"destinationAccountId\":" + destId + ",\"amount\":100}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/accounts/" + sourceId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Soft-deleted: still retrievable by id (history intact), but excluded from the list.
        mockMvc.perform(get("/accounts/" + sourceId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + sourceId + ")]").isEmpty());
    }

    @Test
    void cannotDeleteAnotherUsersAccount() throws Exception {
        String ownerAuth = authHeaderFor("delete-owner@example.com");
        long id = createAccount(ownerAuth, "Conta", "CHECKING", null);

        String otherAuth = authHeaderFor("delete-other@example.com");

        mockMvc.perform(delete("/accounts/" + id).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }
}
