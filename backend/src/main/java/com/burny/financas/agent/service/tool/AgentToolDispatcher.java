package com.burny.financas.agent.service.tool;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.agent.dto.TransactionDraft;
import com.burny.financas.agent.exception.AgentToolExecutionException;
import com.burny.financas.budgets.service.BudgetService;
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
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Routes a Gemini {@code functionCall} to the matching existing service, using the {@code userId}
 * extracted from the authenticated request — never anything the model's arguments claim (design.md
 * Decision 3). An unrecognized tool name or an id that doesn't belong to the caller is returned as
 * an error {@code functionResponse}, not thrown, so Gemini can recover in its next turn.
 */
@Component
@RequiredArgsConstructor
public class AgentToolDispatcher {

    private final ReportService reportService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final PlanningEntryService planningEntryService;
    private final InvestmentPortfolioService investmentPortfolioService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public ToolDispatchResult dispatch(String toolName, Map<String, Object> args, Long userId) {
        Map<String, Object> safeArgs = args == null ? Map.of() : args;
        try {
            return switch (toolName == null ? "" : toolName) {
                case AgentToolCatalog.GET_SPENDING_BY_CATEGORY -> getSpendingByCategory(userId, safeArgs);
                case AgentToolCatalog.GET_BUDGET_STATUS -> getBudgetStatus(userId);
                case AgentToolCatalog.GET_GOAL_STATUS -> getGoalStatus(userId, safeArgs);
                case AgentToolCatalog.GET_PROJECTED_CASH_FLOW -> getProjectedCashFlow(userId, safeArgs);
                case AgentToolCatalog.GET_INVESTMENT_SUMMARY -> getInvestmentSummary(userId);
                case AgentToolCatalog.PROPOSE_TRANSACTION -> proposeTransaction(userId, safeArgs);
                default -> ToolDispatchResult.of(ToolArgs.map("error", "Unknown tool: " + toolName));
            };
        } catch (AccountNotFoundException | CategoryNotFoundException e) {
            return ToolDispatchResult.of(ToolArgs.map("error", e.getMessage()));
        } catch (AgentToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentToolExecutionException("Tool execution failed for " + toolName, e);
        }
    }

    private ToolDispatchResult getSpendingByCategory(Long userId, Map<String, Object> args) {
        LocalDate startDate = ToolArgs.asLocalDate(args.get("startDate"));
        LocalDate endDate = ToolArgs.asLocalDate(args.get("endDate"));
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(today.lengthOfMonth());
        }
        String categoryFilter = ToolArgs.asString(args.get("categoryName"));

        List<CategorySpendingDto> spending = reportService.getSpendingByCategory(userId, startDate, endDate);
        if (categoryFilter != null) {
            String needle = categoryFilter.toLowerCase(Locale.ROOT);
            spending = spending.stream()
                    .filter(dto -> dto.categoryName() != null && dto.categoryName().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        List<Map<String, Object>> categories = spending.stream()
                .map(dto -> ToolArgs.map(
                        "categoryName", dto.categoryName(),
                        "total", dto.total(),
                        "percentage", dto.percentage()))
                .toList();

        return ToolDispatchResult.of(ToolArgs.map(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "categories", categories));
    }

    private ToolDispatchResult getBudgetStatus(Long userId) {
        List<Map<String, Object>> budgets = budgetService.list(userId).stream()
                .map(budget -> ToolArgs.map(
                        "categoryName", budget.categoryName(),
                        "limitAmount", budget.limitAmount(),
                        "spentAmount", budget.spentAmount()))
                .toList();
        return ToolDispatchResult.of(ToolArgs.map("budgets", budgets));
    }

    private ToolDispatchResult getGoalStatus(Long userId, Map<String, Object> args) {
        String nameFilter = ToolArgs.asString(args.get("goalName"));
        List<GoalResponse> goals = goalService.list(userId);
        if (nameFilter != null) {
            String needle = nameFilter.toLowerCase(Locale.ROOT);
            goals = goals.stream().filter(goal -> goal.name().toLowerCase(Locale.ROOT).contains(needle)).toList();
        }

        List<Map<String, Object>> payload = goals.stream()
                .map(goal -> ToolArgs.map(
                        "name", goal.name(),
                        "targetAmount", goal.targetAmount(),
                        "currentAmount", goal.currentAmount(),
                        "percentComplete", goal.percentComplete(),
                        "completed", goal.completed(),
                        "onTrack", goal.onTrack()))
                .toList();
        return ToolDispatchResult.of(ToolArgs.map("goals", payload));
    }

    private ToolDispatchResult getProjectedCashFlow(Long userId, Map<String, Object> args) {
        Long months = ToolArgs.asLong(args.get("months"));
        ProjectedCashFlowResponse response =
                planningEntryService.getProjectedCashFlow(userId, months != null ? months.intValue() : null);

        List<Map<String, Object>> periods = response.periods().stream()
                .map(period -> ToolArgs.map(
                        "month", period.month(),
                        "totalReceivable", period.totalReceivable(),
                        "totalPayable", period.totalPayable(),
                        "projectedBalance", period.projectedBalance()))
                .toList();

        return ToolDispatchResult.of(ToolArgs.map(
                "currentAvailableBalance", response.currentAvailableBalance(),
                "periods", periods));
    }

    private ToolDispatchResult getInvestmentSummary(Long userId) {
        PortfolioSummaryResponse summary = investmentPortfolioService.getPortfolioSummary(userId);
        return ToolDispatchResult.of(ToolArgs.map(
                "totalInvested", summary.totalInvested(),
                "totalCurrentValue", summary.totalCurrentValue(),
                "profitabilityAmount", summary.profitabilityAmount(),
                "profitabilityPercentage", summary.profitabilityPercentage()));
    }

    private ToolDispatchResult proposeTransaction(Long userId, Map<String, Object> args) {
        Long accountId = ToolArgs.asLong(args.get("accountId"));
        String typeText = ToolArgs.asString(args.get("type"));
        BigDecimal amount = ToolArgs.asBigDecimal(args.get("amount"));
        String description = ToolArgs.asString(args.get("description"));
        Long categoryId = ToolArgs.asLong(args.get("categoryId"));
        LocalDate date = ToolArgs.asLocalDate(args.get("date"));

        if (accountId == null || typeText == null || amount == null || description == null) {
            return ToolDispatchResult.of(ToolArgs.map("error",
                    "Missing required fields to propose a transaction: accountId, type, amount and description are all required"));
        }

        TransactionType type;
        try {
            type = TransactionType.valueOf(typeText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ToolDispatchResult.of(ToolArgs.map("error", "Invalid transaction type: " + typeText));
        }

        // Ownership of accountId/categoryId is enforced here by the service call itself (both throw
        // a *NotFoundException for an id that doesn't belong to userId, caught by dispatch() above).
        AccountResponse account = accountService.get(userId, accountId);
        String categoryName = null;
        if (categoryId != null) {
            categoryName = categoryService.get(userId, categoryId).name();
        }

        LocalDate resolvedDate = date != null ? date : LocalDate.now();
        TransactionDraft draft = new TransactionDraft(
                account.id(), account.name(), type, amount, description, categoryId, categoryName, resolvedDate);

        Map<String, Object> payload = ToolArgs.map(
                "status", "draft_ready",
                "accountName", account.name(),
                "type", type.name(),
                "amount", amount,
                "description", description,
                "categoryName", categoryName,
                "date", resolvedDate.toString());
        return ToolDispatchResult.withDraft(payload, draft);
    }
}
