package com.burny.financas.transactions.service;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionRecurrence;
import com.burny.financas.transactions.exception.TransactionNotFoundException;
import com.burny.financas.transactions.repository.TransactionRecurrenceRepository;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates future occurrences of active recurrences and lets a user cancel one. Each occurrence is
 * cloned from the most recently generated transaction in the series (account, category, amount,
 * description) — see design.md's "Recorrência" decision for why the recurring configuration itself
 * doesn't duplicate those fields.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionRecurrenceService {

    private final TransactionRecurrenceRepository transactionRecurrenceRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionBalanceService balanceService;

    /**
     * Runs daily; each due recurrence catches up on every missed scheduled date (not just the
     * earliest one), per "Backlogged occurrences are all generated".
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void generateDueOccurrencesScheduled() {
        generateDueOccurrences();
    }

    @Transactional
    public void generateDueOccurrences() {
        LocalDate today = LocalDate.now();
        List<TransactionRecurrence> dueRecurrences =
                transactionRecurrenceRepository.findAllByActiveTrueAndNextOccurrenceDateLessThanEqual(today);

        for (TransactionRecurrence recurrence : dueRecurrences) {
            while (recurrence.isDue(today)) {
                generateOccurrence(recurrence);
                recurrence.advance();
            }
            transactionRecurrenceRepository.save(recurrence);
        }
    }

    @Transactional
    public void cancel(Long userId, Long id) {
        TransactionRecurrence recurrence = transactionRecurrenceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException("Recurrence not found"));
        recurrence.setActive(false);
        transactionRecurrenceRepository.save(recurrence);
    }

    private void generateOccurrence(TransactionRecurrence recurrence) {
        Transaction lastOccurrence = transactionRepository
                .findTopByRecurrenceIdOrderByTransactionDateDescIdDesc(recurrence.getId())
                .orElseThrow(() -> {
                    log.error("Recurrence {} has no prior occurrence to clone from", recurrence.getId());
                    return new IllegalStateException("Recurrence has no occurrences: " + recurrence.getId());
                });

        Account account = accountRepository.findByIdForUpdate(lastOccurrence.getAccount().getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        Transaction occurrence = Transaction.builder()
                .user(lastOccurrence.getUser())
                .account(account)
                .category(lastOccurrence.getCategory())
                .recurrence(recurrence)
                .type(lastOccurrence.getType())
                .amount(lastOccurrence.getAmount())
                .description(lastOccurrence.getDescription())
                .transactionDate(recurrence.getNextOccurrenceDate())
                .note(lastOccurrence.getNote())
                .active(true)
                .build();

        balanceService.applyEffect(account, occurrence.getType(), occurrence.getAmount());
        accountRepository.save(account);
        transactionRepository.save(occurrence);
    }
}
