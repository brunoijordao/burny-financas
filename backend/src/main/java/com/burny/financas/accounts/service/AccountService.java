package com.burny.financas.accounts.service;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.dto.ConsolidatedBalanceResponse;
import com.burny.financas.accounts.dto.CreateAccountRequest;
import com.burny.financas.accounts.dto.UpdateAccountRequest;
import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.exception.InvalidAccountDataException;
import com.burny.financas.accounts.mapper.AccountMapper;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final AccountService self;

    /**
     * {@code self} is a lazily-resolved proxy back to this same bean, used so
     * {@link #tryHardDelete(Long)} runs through the Spring AOP proxy (needed for its own
     * {@code REQUIRES_NEW} transaction to apply) even though it's called from another method on
     * this class. Mirrors the same pattern in {@code RefreshTokenService}.
     */
    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            AccountMapper accountMapper,
            @Lazy AccountService self
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMapper = accountMapper;
        this.self = self;
    }

    @Transactional
    public AccountResponse create(Long userId, CreateAccountRequest request) {
        if (request.type() == AccountType.CREDIT_CARD && request.creditLimit() == null) {
            throw new InvalidAccountDataException("Credit limit is required for credit card accounts");
        }

        Account account = Account.builder()
                .user(userRepository.getReferenceById(userId))
                .name(request.name())
                .icon(request.icon())
                .color(request.color())
                .type(request.type())
                .balance(BigDecimal.ZERO)
                .creditLimit(request.type() == AccountType.CREDIT_CARD ? request.creditLimit() : null)
                .currentInvoice(request.type() == AccountType.CREDIT_CARD ? BigDecimal.ZERO : null)
                .active(true)
                .build();

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Long userId) {
        return accountRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long userId, Long id) {
        Account account = findOwnedOrThrow(id, userId);
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse update(Long userId, Long id, UpdateAccountRequest request) {
        Account account = findOwnedOrThrow(id, userId);

        account.setName(request.name());
        account.setIcon(request.icon());
        account.setColor(request.color());

        if (account.isCreditCard()) {
            if (request.creditLimit() == null) {
                throw new InvalidAccountDataException("Credit limit is required for credit card accounts");
            }
            account.setCreditLimit(request.creditLimit());
        }

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    /**
     * Tries a hard delete first; if the account has linked history (e.g. transfers) referencing it
     * via a foreign key, falls back to marking it inactive instead. See design.md's "Exclusão"
     * decision for why this is driven by the database constraint rather than an explicit check.
     *
     * <p>The hard-delete attempt runs in its own {@code REQUIRES_NEW} transaction (via the
     * self-proxy) and is only caught here, in the caller's still-active outer transaction: a JPA
     * provider marks its persistence context rollback-only the instant a flush fails, even if the
     * application catches the exception, so the failed attempt must live in a transaction that is
     * allowed to roll back — this outer transaction must not be the one that touched it.
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Account account = findOwnedOrThrow(id, userId);

        try {
            self.hardDelete(account.getId());
        } catch (DataIntegrityViolationException ex) {
            account.setActive(false);
            accountRepository.save(account);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void hardDelete(Long accountId) {
        accountRepository.deleteById(accountId);
        accountRepository.flush();
    }

    @Transactional(readOnly = true)
    public ConsolidatedBalanceResponse getConsolidatedBalance(Long userId) {
        List<Account> accounts = accountRepository.findAllByUserIdAndActiveTrue(userId);

        BigDecimal consolidated = BigDecimal.ZERO;
        for (Account account : accounts) {
            consolidated = account.isCreditCard()
                    ? consolidated.subtract(account.getCurrentInvoice())
                    : consolidated.add(account.getBalance());
        }

        return new ConsolidatedBalanceResponse(consolidated);
    }

    private Account findOwnedOrThrow(Long id, Long userId) {
        return accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }
}
