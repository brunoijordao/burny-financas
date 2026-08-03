package com.burny.financas.agent.service.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.agent.dto.TransactionDraft;
import com.burny.financas.budgets.dto.BudgetResponse;
import com.burny.financas.budgets.service.BudgetService;
import com.burny.financas.categories.dto.CategoryResponse;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.service.CategoryService;
import com.burny.financas.goals.dto.GoalResponse;
import com.burny.financas.goals.service.GoalService;
import com.burny.financas.investments.dto.PortfolioSummaryResponse;
import com.burny.financas.investments.service.InvestmentPortfolioService;
import com.burny.financas.planning.dto.ProjectedCashFlowResponse;
import com.burny.financas.planning.service.PlanningEntryService;
import com.burny.financas.reports.dto.CategorySpendingDto;
import com.burny.financas.reports.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentToolDispatcherTest {

    private static final Long USER_ID = 42L;

    @Mock
    private ReportService reportService;
    @Mock
    private BudgetService budgetService;
    @Mock
    private GoalService goalService;
    @Mock
    private PlanningEntryService planningEntryService;
    @Mock
    private InvestmentPortfolioService investmentPortfolioService;
    @Mock
    private AccountService accountService;
    @Mock
    private CategoryService categoryService;

    private AgentToolDispatcher dispatcher() {
        return new AgentToolDispatcher(
                reportService, budgetService, goalService, planningEntryService,
                investmentPortfolioService, accountService, categoryService);
    }

    private AccountResponse account(Long id, String name) {
        return new AccountResponse(id, name, "wallet", "#000", AccountType.CHECKING, true,
                new BigDecimal("100.00"), null, null, null, null);
    }

    @Test
    void unknownToolReturnsErrorFunctionResponseInsteadOfThrowing() {
        ToolDispatchResult result = dispatcher().dispatch("notARealTool", Map.of(), USER_ID);

        assertThat(result.draft()).isNull();
        assertThat(result.functionResponsePayload()).containsKey("error");
    }

    @Test
    void getBudgetStatusCallsBudgetServiceWithInjectedUserId() {
        when(budgetService.list(USER_ID)).thenReturn(List.of(
                new BudgetResponse(1L, 10L, "Alimentacao", "food", "#111", new BigDecimal("500"), new BigDecimal("120"), null, null, null)));

        ToolDispatchResult result = dispatcher().dispatch(AgentToolCatalog.GET_BUDGET_STATUS, Map.of(), USER_ID);

        verify(budgetService).list(USER_ID);
        assertThat(result.functionResponsePayload()).containsKey("budgets");
    }

    @Test
    void getSpendingByCategoryDefaultsToCurrentMonthAndFiltersByName() {
        when(reportService.getSpendingByCategory(eq(USER_ID), any(), any())).thenReturn(List.of(
                new CategorySpendingDto(1L, "Transporte", "car", "#222", new BigDecimal("80"), new BigDecimal("100")),
                new CategorySpendingDto(2L, "Alimentacao", "food", "#111", new BigDecimal("120"), new BigDecimal("60"))));

        ToolDispatchResult result = dispatcher().dispatch(
                AgentToolCatalog.GET_SPENDING_BY_CATEGORY, Map.of("categoryName", "transporte"), USER_ID);

        verify(reportService).getSpendingByCategory(eq(USER_ID), any(LocalDate.class), any(LocalDate.class));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) result.functionResponsePayload().get("categories");
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0)).containsEntry("categoryName", "Transporte");
    }

    @Test
    void getGoalStatusFiltersByNameWhenProvided() {
        when(goalService.list(USER_ID)).thenReturn(List.of(
                new GoalResponse(1L, "Viagem", new BigDecimal("5000"), LocalDate.now().plusMonths(6),
                        new BigDecimal("1000"), new BigDecimal("20"), false, null, true, null, null),
                new GoalResponse(2L, "Reserva", new BigDecimal("10000"), LocalDate.now().plusYears(1),
                        new BigDecimal("2000"), new BigDecimal("20"), false, null, true, null, null)));

        ToolDispatchResult result = dispatcher().dispatch(
                AgentToolCatalog.GET_GOAL_STATUS, Map.of("goalName", "viagem"), USER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> goals = (List<Map<String, Object>>) result.functionResponsePayload().get("goals");
        assertThat(goals).hasSize(1);
        assertThat(goals.get(0)).containsEntry("name", "Viagem");
    }

    @Test
    void getProjectedCashFlowPassesMonthsArgument() {
        when(planningEntryService.getProjectedCashFlow(USER_ID, 6))
                .thenReturn(new ProjectedCashFlowResponse(new BigDecimal("100"), List.of()));

        dispatcher().dispatch(AgentToolCatalog.GET_PROJECTED_CASH_FLOW, Map.of("months", 6), USER_ID);

        verify(planningEntryService).getProjectedCashFlow(USER_ID, 6);
    }

    @Test
    void getInvestmentSummaryCallsInvestmentPortfolioService() {
        when(investmentPortfolioService.getPortfolioSummary(USER_ID))
                .thenReturn(new PortfolioSummaryResponse(new BigDecimal("1000"), new BigDecimal("1100"), new BigDecimal("100"), new BigDecimal("10")));

        ToolDispatchResult result = dispatcher().dispatch(AgentToolCatalog.GET_INVESTMENT_SUMMARY, Map.of(), USER_ID);

        verify(investmentPortfolioService).getPortfolioSummary(USER_ID);
        assertThat(result.functionResponsePayload()).containsEntry("totalInvested", new BigDecimal("1000"));
    }

    @Test
    void proposeTransactionReturnsDraftWhenAccountAndCategoryAreOwned() {
        when(accountService.get(USER_ID, 7L)).thenReturn(account(7L, "Conta Corrente"));
        when(categoryService.get(USER_ID, 10L)).thenReturn(
                new CategoryResponse(10L, "Alimentacao", "food", "#111", null, false, true, List.of(), null, null));

        ToolDispatchResult result = dispatcher().dispatch(AgentToolCatalog.PROPOSE_TRANSACTION, Map.of(
                "accountId", "7",
                "type", "EXPENSE",
                "amount", 45.5,
                "description", "Fast food",
                "categoryId", "10"
        ), USER_ID);

        assertThat(result.draft()).isNotNull();
        TransactionDraft draft = result.draft();
        assertThat(draft.accountId()).isEqualTo(7L);
        assertThat(draft.accountName()).isEqualTo("Conta Corrente");
        assertThat(draft.categoryName()).isEqualTo("Alimentacao");
        assertThat(draft.amount()).isEqualByComparingTo("45.5");
    }

    @Test
    void proposeTransactionWithMissingRequiredFieldsReturnsErrorWithoutCallingServices() {
        ToolDispatchResult result = dispatcher().dispatch(AgentToolCatalog.PROPOSE_TRANSACTION, Map.of(
                "type", "EXPENSE"
        ), USER_ID);

        assertThat(result.draft()).isNull();
        assertThat(result.functionResponsePayload()).containsKey("error");
        verify(accountService, never()).get(any(), any());
    }

    @Test
    void proposeTransactionWithAccountNotOwnedByCallerReturnsErrorInsteadOfThrowing() {
        when(accountService.get(USER_ID, 999L)).thenThrow(new AccountNotFoundException("Account not found"));

        ToolDispatchResult result = dispatcher().dispatch(AgentToolCatalog.PROPOSE_TRANSACTION, Map.of(
                "accountId", "999",
                "type", "EXPENSE",
                "amount", 10,
                "description", "x"
        ), USER_ID);

        assertThat(result.draft()).isNull();
        assertThat(result.functionResponsePayload()).containsKey("error");
    }

    @Test
    void proposeTransactionWithCategoryNotOwnedByCallerReturnsErrorInsteadOfThrowing() {
        when(accountService.get(USER_ID, 7L)).thenReturn(account(7L, "Conta Corrente"));
        when(categoryService.get(USER_ID, 999L)).thenThrow(new CategoryNotFoundException("Category not found"));

        ToolDispatchResult result = dispatcher().dispatch(AgentToolCatalog.PROPOSE_TRANSACTION, Map.of(
                "accountId", "7",
                "type", "EXPENSE",
                "amount", 10,
                "description", "x",
                "categoryId", "999"
        ), USER_ID);

        assertThat(result.draft()).isNull();
        assertThat(result.functionResponsePayload()).containsKey("error");
    }
}
