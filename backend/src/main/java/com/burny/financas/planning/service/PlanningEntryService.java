package com.burny.financas.planning.service;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.planning.dto.CreatePlanningEntryRequest;
import com.burny.financas.planning.dto.PlanningEntryResponse;
import com.burny.financas.planning.dto.ProjectedCashFlowPeriod;
import com.burny.financas.planning.dto.ProjectedCashFlowResponse;
import com.burny.financas.planning.dto.UpdatePlanningEntryRequest;
import com.burny.financas.planning.entity.PlanningEntry;
import com.burny.financas.planning.entity.PlanningEntryStatus;
import com.burny.financas.planning.entity.PlanningEntryType;
import com.burny.financas.planning.exception.InvalidPlanningEntryDataException;
import com.burny.financas.planning.exception.PlanningEntryNotFoundException;
import com.burny.financas.planning.mapper.PlanningEntryMapper;
import com.burny.financas.planning.repository.PlanningEntryRepository;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import com.burny.financas.transactions.service.TransactionBalanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settlement/undo reuse {@link TransactionBalanceService} exactly as {@code
 * PdfImportService.confirmItem} does, so balance-effect math is never duplicated here
 * (design.md Decision 3).
 */
@Service
@RequiredArgsConstructor
public class PlanningEntryService {

    private static final int DEFAULT_PROJECTION_MONTHS = 3;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PlanningEntryRepository planningEntryRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionBalanceService transactionBalanceService;
    private final AccountService accountService;
    private final PlanningEntryMapper planningEntryMapper;

    @Transactional
    public PlanningEntryResponse create(Long userId, CreatePlanningEntryRequest request) {
        Account account = findOwnedAccountOrThrow(userId, request.accountId());
        Category category = findOwnedCategoryOrThrow(userId, request.categoryId());

        PlanningEntry entry = PlanningEntry.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .category(category)
                .type(request.type())
                .amount(request.amount())
                .description(request.description())
                .dueDate(request.dueDate())
                .status(PlanningEntryStatus.PENDING)
                .active(true)
                .build();
        return planningEntryMapper.toResponse(planningEntryRepository.save(entry));
    }

    @Transactional
    public PlanningEntryResponse update(Long userId, Long id, UpdatePlanningEntryRequest request) {
        PlanningEntry entry = findOwnedOrThrow(userId, id);
        Account account = findOwnedAccountOrThrow(userId, request.accountId());
        Category category = findOwnedCategoryOrThrow(userId, request.categoryId());

        entry.setType(request.type());
        entry.setAccount(account);
        entry.setCategory(category);
        entry.setAmount(request.amount());
        entry.setDescription(request.description());
        entry.setDueDate(request.dueDate());
        return planningEntryMapper.toResponse(planningEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        PlanningEntry entry = findOwnedOrThrow(userId, id);
        entry.setActive(false);
        planningEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PlanningEntryResponse get(Long userId, Long id) {
        return planningEntryMapper.toResponse(findOwnedOrThrow(userId, id));
    }

    @Transactional(readOnly = true)
    public List<PlanningEntryResponse> list(Long userId) {
        return planningEntryRepository.findAllByUserIdAndActiveTrueOrderByDueDateAsc(userId).stream()
                .map(planningEntryMapper::toResponse)
                .toList();
    }

    @Transactional
    public PlanningEntryResponse settle(Long userId, Long id, LocalDate settlementDate) {
        PlanningEntry entry = findOwnedOrThrow(userId, id);
        if (entry.getStatus() == PlanningEntryStatus.SETTLED) {
            throw new InvalidPlanningEntryDataException("Planning entry is already settled");
        }

        Account account = accountRepository.findByIdForUpdate(entry.getAccount().getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        TransactionType transactionType = entry.getType() == PlanningEntryType.PAYABLE
                ? TransactionType.EXPENSE
                : TransactionType.INCOME;

        transactionBalanceService.applyEffect(account, transactionType, entry.getAmount());
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .category(entry.getCategory())
                .type(transactionType)
                .amount(entry.getAmount())
                .description(entry.getDescription())
                .transactionDate(settlementDate != null ? settlementDate : LocalDate.now())
                .active(true)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        entry.setStatus(PlanningEntryStatus.SETTLED);
        entry.setSettledAt(LocalDateTime.now());
        entry.setTransaction(savedTransaction);
        return planningEntryMapper.toResponse(planningEntryRepository.save(entry));
    }

    @Transactional
    public PlanningEntryResponse undoSettlement(Long userId, Long id) {
        PlanningEntry entry = findOwnedOrThrow(userId, id);
        if (entry.getStatus() != PlanningEntryStatus.SETTLED) {
            throw new InvalidPlanningEntryDataException("Planning entry is not settled");
        }

        Transaction transaction = entry.getTransaction();
        Account account = accountRepository.findByIdForUpdate(transaction.getAccount().getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        transactionBalanceService.reverseEffect(account, transaction.getType(), transaction.getAmount());
        accountRepository.save(account);

        transaction.setActive(false);
        transactionRepository.save(transaction);

        entry.setStatus(PlanningEntryStatus.PENDING);
        entry.setSettledAt(null);
        entry.setTransaction(null);
        return planningEntryMapper.toResponse(planningEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<PlanningEntryResponse> getCalendar(Long userId, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        return planningEntryRepository
                .findAllByUserIdAndActiveTrueAndDueDateBetweenOrderByDueDateAsc(userId, monthStart, monthEnd)
                .stream()
                .map(planningEntryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectedCashFlowResponse getProjectedCashFlow(Long userId, Integer months) {
        int resolvedMonths = months != null ? months : DEFAULT_PROJECTION_MONTHS;
        BigDecimal currentBalance = accountService.getConsolidatedBalance(userId).consolidatedBalance();

        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.plusDays(1);
        LocalDate windowEnd = today.plusMonths(resolvedMonths).with(TemporalAdjusters.lastDayOfMonth());

        List<PlanningEntry> pendingEntries = planningEntryRepository
                .findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
                        userId, PlanningEntryStatus.PENDING, windowStart, windowEnd);

        List<ProjectedCashFlowPeriod> periods = new ArrayList<>();
        BigDecimal runningBalance = currentBalance;
        for (int i = 1; i <= resolvedMonths; i++) {
            YearMonth periodMonth = YearMonth.from(today).plusMonths(i);
            BigDecimal totalReceivable = sumByTypeInMonth(pendingEntries, PlanningEntryType.RECEIVABLE, periodMonth);
            BigDecimal totalPayable = sumByTypeInMonth(pendingEntries, PlanningEntryType.PAYABLE, periodMonth);
            runningBalance = runningBalance.add(totalReceivable).subtract(totalPayable);
            periods.add(new ProjectedCashFlowPeriod(
                    periodMonth.format(MONTH_FORMATTER), totalReceivable, totalPayable, runningBalance));
        }

        return new ProjectedCashFlowResponse(currentBalance, periods);
    }

    private BigDecimal sumByTypeInMonth(List<PlanningEntry> entries, PlanningEntryType type, YearMonth month) {
        return entries.stream()
                .filter(entry -> entry.getType() == type && YearMonth.from(entry.getDueDate()).equals(month))
                .map(PlanningEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PlanningEntry findOwnedOrThrow(Long userId, Long id) {
        return planningEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new PlanningEntryNotFoundException("Planning entry not found"));
    }

    private Account findOwnedAccountOrThrow(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private Category findOwnedCategoryOrThrow(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }
}
