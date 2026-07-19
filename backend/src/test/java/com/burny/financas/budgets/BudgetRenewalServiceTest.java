package com.burny.financas.budgets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.budgets.entity.Budget;
import com.burny.financas.budgets.repository.BudgetRepository;
import com.burny.financas.budgets.service.BudgetRenewalService;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class BudgetRenewalServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetRenewalService budgetRenewalService;

    @Autowired
    private JwtService jwtService;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
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

    private Budget seedBudget(String email, long categoryId, LocalDate month, BigDecimal limitAmount) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId()).orElseThrow();
        return budgetRepository.save(Budget.builder()
                .user(user)
                .category(category)
                .budgetMonth(month)
                .limitAmount(limitAmount)
                .active(true)
                .build());
    }

    @Test
    void previouslyBudgetedCategoryGetsABlankBudgetThisMonth() throws Exception {
        String email = "renewal-blank@example.com";
        String auth = authHeaderFor(email);
        long categoryId = createCategory(auth, "Alimentacao");
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate previousMonth = currentMonth.minusMonths(1);
        seedBudget(email, categoryId, previousMonth, new BigDecimal("800.00"));

        budgetRenewalService.renewDueBudgets();

        User user = userRepository.findByEmail(email).orElseThrow();
        Budget renewed = budgetRepository
                .findByUserIdAndCategoryIdAndBudgetMonth(user.getId(), categoryId, currentMonth)
                .orElseThrow();
        assertThat(renewed.getLimitAmount()).isNull();
        assertThat(renewed.isActive()).isTrue();
    }

    @Test
    void categoryAlreadyBudgetedThisMonthIsNotOverwritten() throws Exception {
        String email = "renewal-untouched@example.com";
        String auth = authHeaderFor(email);
        long categoryId = createCategory(auth, "Alimentacao");
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate previousMonth = currentMonth.minusMonths(1);
        seedBudget(email, categoryId, previousMonth, new BigDecimal("800.00"));

        mockMvc.perform(put("/budgets/categories/" + categoryId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":950}"))
                .andExpect(status().isOk());

        budgetRenewalService.renewDueBudgets();

        User user = userRepository.findByEmail(email).orElseThrow();
        List<Budget> currentMonthBudgets = budgetRepository
                .findByUserIdAndActiveAndBudgetMonth(user.getId(), true, currentMonth);
        assertThat(currentMonthBudgets).hasSize(1);
        assertThat(currentMonthBudgets.get(0).getLimitAmount()).isEqualByComparingTo("950.00");
    }

    @Test
    void renewalIsIdempotent() throws Exception {
        String email = "renewal-idempotent@example.com";
        String auth = authHeaderFor(email);
        long categoryId = createCategory(auth, "Alimentacao");
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate previousMonth = currentMonth.minusMonths(1);
        seedBudget(email, categoryId, previousMonth, new BigDecimal("800.00"));

        budgetRenewalService.renewDueBudgets();
        budgetRenewalService.renewDueBudgets();

        User user = userRepository.findByEmail(email).orElseThrow();
        List<Budget> currentMonthBudgets = budgetRepository
                .findByUserIdAndActiveAndBudgetMonth(user.getId(), true, currentMonth);
        assertThat(currentMonthBudgets).hasSize(1);
    }
}
