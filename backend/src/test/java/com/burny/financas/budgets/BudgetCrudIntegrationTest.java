package com.burny.financas.budgets;

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
class BudgetCrudIntegrationTest {

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

    private void createTransaction(String authHeader, String type, String amount, long accountId, Long categoryId) throws Exception {
        String body = "{"
                + "\"description\":\"X\","
                + "\"amount\":" + amount + ","
                + "\"type\":\"" + type + "\","
                + "\"transactionDate\":\"" + LocalDate.now() + "\","
                + "\"accountId\":" + accountId
                + (categoryId != null ? ",\"categoryId\":" + categoryId : "")
                + "}";
        mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private long setBudget(String authHeader, long categoryId, String limitAmount) throws Exception {
        String response = mockMvc.perform(put("/budgets/categories/" + categoryId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":" + limitAmount + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void settingABudgetForTheFirstTimeCreatesIt() throws Exception {
        String auth = authHeaderFor("budget-create@example.com");
        long categoryId = createCategory(auth, "Alimentacao");

        mockMvc.perform(put("/budgets/categories/" + categoryId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":800}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", is((int) categoryId)))
                .andExpect(jsonPath("$.limitAmount", is(800)))
                .andExpect(jsonPath("$.spentAmount", is(0)));
    }

    @Test
    void settingABudgetAgainUpdatesTheExistingOne() throws Exception {
        String auth = authHeaderFor("budget-update@example.com");
        long categoryId = createCategory(auth, "Alimentacao");

        long firstId = setBudget(auth, categoryId, "800");
        long secondId = setBudget(auth, categoryId, "950");

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);

        mockMvc.perform(get("/budgets").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].limitAmount", is(950)));
    }

    @Test
    void categoryMustBelongToCaller() throws Exception {
        String ownerAuth = authHeaderFor("budget-category-owner@example.com");
        long categoryId = createCategory(ownerAuth, "Alimentacao");

        String otherAuth = authHeaderFor("budget-category-other@example.com");

        mockMvc.perform(put("/budgets/categories/" + categoryId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":800}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void spentAmountReflectsExpenseTransactionsAndExcludesIncome() throws Exception {
        String auth = authHeaderFor("budget-spent@example.com");
        long accountId = createAccount(auth);
        long categoryId = createCategory(auth, "Alimentacao");
        setBudget(auth, categoryId, "800");

        createTransaction(auth, "EXPENSE", "200.00", accountId, categoryId);
        createTransaction(auth, "EXPENSE", "150.00", accountId, categoryId);
        createTransaction(auth, "INCOME", "1000.00", accountId, categoryId);

        mockMvc.perform(get("/budgets").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spentAmount", is(350.0)));
    }

    @Test
    void listingExcludesCategoriesWithNoBudgetThisMonth() throws Exception {
        String auth = authHeaderFor("budget-no-budget-category@example.com");
        createCategory(auth, "SemOrcamento");

        mockMvc.perform(get("/budgets").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void listingIsIsolatedPerUser() throws Exception {
        String ownerAuth = authHeaderFor("budget-list-owner@example.com");
        long ownerCategoryId = createCategory(ownerAuth, "Alimentacao");
        setBudget(ownerAuth, ownerCategoryId, "800");

        String otherAuth = authHeaderFor("budget-list-other@example.com");
        long otherCategoryId = createCategory(otherAuth, "Transporte");
        setBudget(otherAuth, otherCategoryId, "300");

        mockMvc.perform(get("/budgets").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].categoryId", is((int) ownerCategoryId)));
    }

    @Test
    void deletingABudgetSoftDeletesItAndExcludesFromListing() throws Exception {
        String auth = authHeaderFor("budget-delete@example.com");
        long categoryId = createCategory(auth, "Alimentacao");
        long budgetId = setBudget(auth, categoryId, "800");

        mockMvc.perform(delete("/budgets/" + budgetId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/budgets").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void nonOwnerCannotDeleteAnotherUsersBudget() throws Exception {
        String ownerAuth = authHeaderFor("budget-delete-owner@example.com");
        long categoryId = createCategory(ownerAuth, "Alimentacao");
        long budgetId = setBudget(ownerAuth, categoryId, "800");

        String otherAuth = authHeaderFor("budget-delete-other@example.com");

        mockMvc.perform(delete("/budgets/" + budgetId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }
}
