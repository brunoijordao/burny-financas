package com.burny.financas.transactions.service;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.categories.service.CategoryResolutionService;
import com.burny.financas.transactions.dto.CreateTransactionRequest;
import com.burny.financas.transactions.dto.PagedResponse;
import com.burny.financas.transactions.dto.TransactionResponse;
import com.burny.financas.transactions.dto.UpdateTransactionRequest;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionRecurrence;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.exception.InvalidTransactionDataException;
import com.burny.financas.transactions.exception.TransactionNotFoundException;
import com.burny.financas.transactions.mapper.TransactionMapper;
import com.burny.financas.transactions.repository.TransactionRecurrenceRepository;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionBalanceService balanceService;
    private final TransactionMapper transactionMapper;
    private final CategoryResolutionService categoryResolutionService;
    private final TransactionRecurrenceRepository transactionRecurrenceRepository;

    @Transactional
    public TransactionResponse create(Long userId, CreateTransactionRequest request) {
        Account account = lockAccountOrThrow(request.accountId());
        requireOwnedBy(account, userId);

        Category category = resolveCategoryForCreate(userId, request);
        TransactionRecurrence recurrence = createRecurrenceIfRequested(userId, request);

        Transaction transaction = Transaction.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .category(category)
                .recurrence(recurrence)
                .type(request.type())
                .amount(request.amount())
                .description(request.description())
                .transactionDate(request.transactionDate())
                .note(request.note())
                .active(true)
                .build();

        balanceService.applyEffect(account, request.type(), request.amount());
        accountRepository.save(account);

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> list(
            Long userId,
            Long accountId,
            Long categoryId,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Transaction> result = transactionRepository.findFiltered(
                userId, true, accountId, categoryId, type, startDate, endDate, pageRequest);
        return PagedResponse.from(result.map(transactionMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long id) {
        return transactionMapper.toResponse(findOwnedOrThrow(id, userId));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, UpdateTransactionRequest request) {
        Transaction transaction = findOwnedOrThrow(id, userId);

        Long oldAccountId = transaction.getAccount().getId();
        Long newAccountId = request.accountId();

        Account oldAccount;
        Account newAccount;
        if (oldAccountId.equals(newAccountId)) {
            oldAccount = lockAccountOrThrow(oldAccountId);
            newAccount = oldAccount;
        } else {
            Long firstId = oldAccountId.compareTo(newAccountId) <= 0 ? oldAccountId : newAccountId;
            Long secondId = oldAccountId.compareTo(newAccountId) <= 0 ? newAccountId : oldAccountId;
            Account first = lockAccountOrThrow(firstId);
            Account second = lockAccountOrThrow(secondId);
            oldAccount = first.getId().equals(oldAccountId) ? first : second;
            newAccount = first.getId().equals(newAccountId) ? first : second;
        }

        requireOwnedBy(newAccount, userId);

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        }

        balanceService.reverseEffect(oldAccount, transaction.getType(), transaction.getAmount());
        balanceService.applyEffect(newAccount, request.type(), request.amount());

        accountRepository.save(oldAccount);
        if (!newAccount.getId().equals(oldAccount.getId())) {
            accountRepository.save(newAccount);
        }

        transaction.setAccount(newAccount);
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setNote(request.note());

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Transaction transaction = findOwnedOrThrow(id, userId);
        Account account = lockAccountOrThrow(transaction.getAccount().getId());

        balanceService.reverseEffect(account, transaction.getType(), transaction.getAmount());
        accountRepository.save(account);

        transaction.setActive(false);
        transactionRepository.save(transaction);
    }

    /**
     * When no category is supplied, falls back to best-effort keyword resolution from the
     * description; a non-match leaves the transaction uncategorized rather than being an error (see
     * "Automatic Category Suggestion").
     */
    private Category resolveCategoryForCreate(Long userId, CreateTransactionRequest request) {
        if (request.categoryId() != null) {
            return categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        }
        return categoryResolutionService.resolve(userId, request.description()).orElse(null);
    }

    /**
     * Persists the recurrence row first so it has an id before the first-occurrence transaction is
     * built to reference it (no cascade is defined on {@code Transaction.recurrence}).
     */
    private TransactionRecurrence createRecurrenceIfRequested(Long userId, CreateTransactionRequest request) {
        if (!request.recurring()) {
            return null;
        }
        if (request.frequency() == null || request.startDate() == null) {
            throw new InvalidTransactionDataException(
                    "Frequency and start date are required for a recurring transaction");
        }

        TransactionRecurrence recurrence = TransactionRecurrence.builder()
                .user(userRepository.getReferenceById(userId))
                .frequency(request.frequency())
                .startDate(request.startDate())
                .nextOccurrenceDate(request.startDate())
                .endDate(request.endDate())
                .active(true)
                .build();
        recurrence.advance();

        return transactionRecurrenceRepository.save(recurrence);
    }

    private Transaction findOwnedOrThrow(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }

    private Account lockAccountOrThrow(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private void requireOwnedBy(Account account, Long userId) {
        if (!account.getUser().getId().equals(userId)) {
            throw new AccountNotFoundException("Account not found");
        }
    }
}
