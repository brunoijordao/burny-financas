package com.burny.financas.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.dto.ConsolidatedBalanceResponse;
import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.planning.dto.CreatePlanningEntryRequest;
import com.burny.financas.planning.dto.PlanningEntryResponse;
import com.burny.financas.planning.dto.ProjectedCashFlowResponse;
import com.burny.financas.planning.dto.UpdatePlanningEntryRequest;
import com.burny.financas.planning.entity.PlanningEntry;
import com.burny.financas.planning.entity.PlanningEntryStatus;
import com.burny.financas.planning.entity.PlanningEntryType;
import com.burny.financas.planning.exception.InvalidPlanningEntryDataException;
import com.burny.financas.planning.exception.PlanningEntryNotFoundException;
import com.burny.financas.planning.mapper.PlanningEntryMapperImpl;
import com.burny.financas.planning.repository.PlanningEntryRepository;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import com.burny.financas.transactions.service.TransactionBalanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanningEntryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PlanningEntryRepository planningEntryRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionBalanceService transactionBalanceService;
    @Mock
    private AccountService accountService;

    private PlanningEntryService service() {
        return new PlanningEntryService(
                planningEntryRepository, accountRepository, categoryRepository, userRepository,
                transactionRepository, transactionBalanceService, accountService, new PlanningEntryMapperImpl());
    }

    private Account account(Long id, AccountType type, String balance) {
        return Account.builder().id(id).type(type).balance(new BigDecimal(balance)).build();
    }

    private PlanningEntry pendingEntry(PlanningEntryType type, String amount, LocalDate dueDate, Account account) {
        return PlanningEntry.builder()
                .id(10L)
                .account(account)
                .type(type)
                .amount(new BigDecimal(amount))
                .description("Conta de luz")
                .dueDate(dueDate)
                .status(PlanningEntryStatus.PENDING)
                .active(true)
                .build();
    }

    @Test
    void createDoesNotAffectAccountBalance() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        when(accountRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(acc));
        when(planningEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningEntryResponse response = service().create(
                USER_ID,
                new CreatePlanningEntryRequest(
                        PlanningEntryType.PAYABLE, 2L, null, new BigDecimal("200.00"), "Aluguel", LocalDate.now().plusDays(5)));

        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(acc.getBalance()).isEqualByComparingTo("1000.00");
        verify(transactionBalanceService, never()).applyEffect(any(), any(), any());
    }

    @Test
    void createRejectsAccountNotOwnedByCaller() {
        when(accountRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(
                USER_ID,
                new CreatePlanningEntryRequest(
                        PlanningEntryType.PAYABLE, 2L, null, new BigDecimal("200.00"), "Aluguel", LocalDate.now())))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void deleteSoftDeletesEntry() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "100.00", LocalDate.now(), acc);
        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(entry));
        when(planningEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().delete(USER_ID, 10L);

        assertThat(entry.isActive()).isFalse();
    }

    @Test
    void updateRejectsEntryNotOwnedByCaller() {
        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(
                USER_ID,
                10L,
                new UpdatePlanningEntryRequest(
                        PlanningEntryType.PAYABLE, 2L, null, new BigDecimal("100.00"), "x", LocalDate.now())))
                .isInstanceOf(PlanningEntryNotFoundException.class);
    }

    @Test
    void settlingPayableCreatesExpenseTransactionAndReducesBalance() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "150.00", LocalDate.now(), acc);
        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(entry));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(acc));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(99L);
            return t;
        });
        when(planningEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // Real TransactionBalanceService semantics needed for the balance assertion below.
        transactionBalanceServiceAppliesRealEffect(acc);

        PlanningEntryResponse response = service().settle(USER_ID, 10L, null);

        assertThat(response.status().name()).isEqualTo("SETTLED");
        assertThat(response.transactionId()).isEqualTo(99L);
        assertThat(acc.getBalance()).isEqualByComparingTo("850.00");
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void settlingReceivableCreatesIncomeTransaction() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        PlanningEntry entry = pendingEntry(PlanningEntryType.RECEIVABLE, "300.00", LocalDate.now(), acc);
        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(entry));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(acc));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planningEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().settle(USER_ID, 10L, null);

        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        verify(transactionBalanceService).applyEffect(any(), typeCaptor.capture(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void settlingAlreadySettledEntryIsRejected() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "150.00", LocalDate.now(), acc);
        entry.setStatus(PlanningEntryStatus.SETTLED);
        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service().settle(USER_ID, 10L, null))
                .isInstanceOf(InvalidPlanningEntryDataException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void undoingSettlementReversesEffectAndSoftDeletesTransaction() {
        Account acc = account(2L, AccountType.CHECKING, "850.00");
        Transaction linkedTransaction = Transaction.builder()
                .id(99L).account(acc).type(TransactionType.EXPENSE).amount(new BigDecimal("150.00")).active(true).build();
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "150.00", LocalDate.now(), acc);
        entry.setStatus(PlanningEntryStatus.SETTLED);
        entry.setTransaction(linkedTransaction);

        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(entry));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(acc));
        when(planningEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningEntryResponse response = service().undoSettlement(USER_ID, 10L);

        verify(transactionBalanceService).reverseEffect(acc, TransactionType.EXPENSE, new BigDecimal("150.00"));
        assertThat(linkedTransaction.isActive()).isFalse();
        verify(transactionRepository).save(linkedTransaction);
        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(response.transactionId()).isNull();
    }

    @Test
    void undoingAnEntryThatWasNeverSettledIsRejected() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "150.00", LocalDate.now(), acc);
        when(planningEntryRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service().undoSettlement(USER_ID, 10L))
                .isInstanceOf(InvalidPlanningEntryDataException.class);
    }

    @Test
    void entryDueTodayIsPendingNotOverdue() {
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "10.00", LocalDate.now(), account(2L, AccountType.CHECKING, "0"));
        assertThat(entry.isOverdue()).isFalse();
    }

    @Test
    void entryPastDueDateIsOverdue() {
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "10.00", LocalDate.now().minusDays(1), account(2L, AccountType.CHECKING, "0"));
        assertThat(entry.isOverdue()).isTrue();
    }

    @Test
    void settledEntryIsNeverOverdueRegardlessOfDueDate() {
        PlanningEntry entry = pendingEntry(PlanningEntryType.PAYABLE, "10.00", LocalDate.now().minusDays(30), account(2L, AccountType.CHECKING, "0"));
        entry.setStatus(PlanningEntryStatus.SETTLED);
        assertThat(entry.isOverdue()).isFalse();
    }

    @Test
    void projectionWithNoPendingEntriesKeepsBalanceFlat() {
        when(accountService.getConsolidatedBalance(USER_ID)).thenReturn(new ConsolidatedBalanceResponse(new BigDecimal("1000.00")));
        when(planningEntryRepository.findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
                any(), any(), any(), any())).thenReturn(List.of());

        ProjectedCashFlowResponse response = service().getProjectedCashFlow(USER_ID, 3);

        assertThat(response.currentAvailableBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.periods()).hasSize(3);
        response.periods().forEach(period -> assertThat(period.projectedBalance()).isEqualByComparingTo("1000.00"));
    }

    @Test
    void pendingReceivableIncreasesNextMonthProjection() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        LocalDate nextMonthDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        PlanningEntry receivable = pendingEntry(PlanningEntryType.RECEIVABLE, "500.00", nextMonthDueDate, acc);

        when(accountService.getConsolidatedBalance(USER_ID)).thenReturn(new ConsolidatedBalanceResponse(new BigDecimal("1000.00")));
        when(planningEntryRepository.findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
                any(), any(), any(), any())).thenReturn(List.of(receivable));

        ProjectedCashFlowResponse response = service().getProjectedCashFlow(USER_ID, 3);

        String nextMonthKey = YearMonth.from(nextMonthDueDate).toString();
        assertThat(response.periods().stream().filter(p -> p.month().equals(nextMonthKey)).findFirst().orElseThrow().projectedBalance())
                .isEqualByComparingTo("1500.00");
    }

    @Test
    void pendingPayableDecreasesNextMonthProjection() {
        Account acc = account(2L, AccountType.CHECKING, "1000.00");
        LocalDate nextMonthDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        PlanningEntry payable = pendingEntry(PlanningEntryType.PAYABLE, "300.00", nextMonthDueDate, acc);

        when(accountService.getConsolidatedBalance(USER_ID)).thenReturn(new ConsolidatedBalanceResponse(new BigDecimal("1000.00")));
        when(planningEntryRepository.findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
                any(), any(), any(), any())).thenReturn(List.of(payable));

        ProjectedCashFlowResponse response = service().getProjectedCashFlow(USER_ID, 3);

        String nextMonthKey = YearMonth.from(nextMonthDueDate).toString();
        assertThat(response.periods().stream().filter(p -> p.month().equals(nextMonthKey)).findFirst().orElseThrow().projectedBalance())
                .isEqualByComparingTo("700.00");
    }

    @Test
    void projectionQueryOnlyRequestsPendingEntriesSoSettledOnesAreExcludedByConstruction() {
        when(accountService.getConsolidatedBalance(USER_ID)).thenReturn(new ConsolidatedBalanceResponse(new BigDecimal("1000.00")));
        when(planningEntryRepository.findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
                any(), any(), any(), any())).thenReturn(List.of());

        service().getProjectedCashFlow(USER_ID, 3);

        ArgumentCaptor<PlanningEntryStatus> statusCaptor = ArgumentCaptor.forClass(PlanningEntryStatus.class);
        verify(planningEntryRepository).findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
                any(), statusCaptor.capture(), any(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(PlanningEntryStatus.PENDING);
    }

    private void transactionBalanceServiceAppliesRealEffect(Account acc) {
        TransactionBalanceService real = new TransactionBalanceService();
        org.mockito.Mockito.doAnswer(invocation -> {
            real.applyEffect(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(transactionBalanceService).applyEffect(any(), any(), any());
    }
}
