package com.burny.financas.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.dto.ConsolidatedBalanceResponse;
import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.investments.dto.NetWorthEvolutionPoint;
import com.burny.financas.investments.service.InvestmentPortfolioService;
import com.burny.financas.reports.dto.CategorySpendingDto;
import com.burny.financas.reports.dto.NetWorthEvolutionReportDto;
import com.burny.financas.reports.dto.StatementLineDto;
import com.burny.financas.reports.exception.InvalidReportRequestException;
import com.burny.financas.reports.mapper.ReportMapperImpl;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private InvestmentPortfolioService investmentPortfolioService;

    private ReportService service() {
        return new ReportService(transactionRepository, accountService, investmentPortfolioService, new ReportMapperImpl());
    }

    private Account account(Long id, String name) {
        return Account.builder().id(id).name(name).type(AccountType.CHECKING).build();
    }

    private Category category(Long id, String name) {
        return Category.builder().id(id).name(name).icon("icon").color("#000").build();
    }

    private Transaction transaction(Account account, Category category, TransactionType type, String amount, LocalDate date) {
        return Transaction.builder()
                .id(1L).account(account).category(category).type(type)
                .amount(new BigDecimal(amount)).description("desc").transactionDate(date).active(true).build();
    }

    @Test
    void statementDelegatesToFilteredTransactionQueryAndMapsResults() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Account acc = account(10L, "Conta Corrente");
        Category cat = category(20L, "Mercado");
        Transaction tx = transaction(acc, cat, TransactionType.EXPENSE, "150.00", start.plusDays(4));

        when(transactionRepository.findFiltered(
                eq(USER_ID), eq(true), eq(10L), eq(20L), eq(TransactionType.EXPENSE), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        List<StatementLineDto> result = service().getStatement(USER_ID, start, end, 10L, 20L, TransactionType.EXPENSE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).accountName()).isEqualTo("Conta Corrente");
        assertThat(result.get(0).categoryName()).isEqualTo("Mercado");
        assertThat(result.get(0).amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void statementUncategorizedTransactionReportsSemCategoriaLabel() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Account acc = account(10L, "Conta Corrente");
        Transaction tx = transaction(acc, null, TransactionType.EXPENSE, "50.00", start);

        when(transactionRepository.findFiltered(
                eq(USER_ID), eq(true), isNull(), isNull(), isNull(), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        List<StatementLineDto> result = service().getStatement(USER_ID, start, end, null, null, null);

        assertThat(result.get(0).categoryName()).isEqualTo("Sem categoria");
    }

    @Test
    void statementRejectsInvertedDateRange() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service().getStatement(USER_ID, start, end, null, null, null))
                .isInstanceOf(InvalidReportRequestException.class);
    }

    @Test
    void spendingByCategoryComputesTotalsAndPercentages() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumExpenseByCategoryForDateRange(USER_ID, true, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of(
                        projection(1L, "Mercado", "icon", "#111", "600"),
                        projection(2L, "Transporte", "icon", "#222", "400")));

        List<CategorySpendingDto> result = service().getSpendingByCategory(USER_ID, start, end);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).percentage()).isEqualByComparingTo("60.00");
        assertThat(result.get(1).percentage()).isEqualByComparingTo("40.00");
    }

    @Test
    void spendingByCategoryUncategorizedGroupUsesFallbackLabel() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumExpenseByCategoryForDateRange(USER_ID, true, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of(projection(null, null, null, null, "100")));

        List<CategorySpendingDto> result = service().getSpendingByCategory(USER_ID, start, end);

        assertThat(result.get(0).categoryName()).isEqualTo("Sem categoria");
        assertThat(result.get(0).percentage()).isEqualByComparingTo("100.00");
    }

    @Test
    void netWorthEvolutionCombinesAccountBalanceAndInvestmentSeriesWithoutRecomputing() {
        when(accountService.getConsolidatedBalance(USER_ID)).thenReturn(new ConsolidatedBalanceResponse(new BigDecimal("1000")));
        List<NetWorthEvolutionPoint> series = List.of(new NetWorthEvolutionPoint(LocalDate.of(2026, 1, 1), new BigDecimal("500")));
        when(investmentPortfolioService.getNetWorthEvolution(USER_ID)).thenReturn(series);

        NetWorthEvolutionReportDto result = service().getNetWorthEvolution(USER_ID);

        assertThat(result.currentConsolidatedAccountBalance()).isEqualByComparingTo("1000");
        assertThat(result.investmentNetWorthEvolution()).isEqualTo(series);
        verify(accountService).getConsolidatedBalance(USER_ID);
        verify(investmentPortfolioService).getNetWorthEvolution(USER_ID);
    }

    private TransactionRepository.CategoryExpenseProjection projection(
            Long categoryId, String categoryName, String icon, String color, String total) {
        return new TransactionRepository.CategoryExpenseProjection() {
            public Long getCategoryId() {
                return categoryId;
            }

            public String getCategoryName() {
                return categoryName;
            }

            public String getIcon() {
                return icon;
            }

            public String getColor() {
                return color;
            }

            public BigDecimal getTotal() {
                return new BigDecimal(total);
            }
        };
    }
}
