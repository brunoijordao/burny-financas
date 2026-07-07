package com.burny.financas.accounts.service;

import com.burny.financas.accounts.dto.TransferRequest;
import com.burny.financas.accounts.dto.TransferResponse;
import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountTransfer;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.exception.TransferNotAllowedException;
import com.burny.financas.accounts.mapper.AccountMapper;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.accounts.repository.AccountTransferRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns atomic transfers between two accounts of the same user. Both accounts are locked
 * (PESSIMISTIC_WRITE) in ascending id order before any balance is read or mutated, so concurrent
 * transfers sharing an account serialize instead of racing, and never deadlock on lock order.
 */
@Service
@RequiredArgsConstructor
public class AccountTransferService {

    private final AccountRepository accountRepository;
    private final AccountTransferRepository accountTransferRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public TransferResponse transfer(Long userId, TransferRequest request) {
        Long sourceId = request.sourceAccountId();
        Long destinationId = request.destinationAccountId();
        BigDecimal amount = request.amount();

        Long firstId = sourceId.compareTo(destinationId) <= 0 ? sourceId : destinationId;
        Long secondId = sourceId.compareTo(destinationId) <= 0 ? destinationId : sourceId;

        Account first = lockOrThrow(firstId);
        Account second = firstId.equals(secondId) ? first : lockOrThrow(secondId);

        Account source = first.getId().equals(sourceId) ? first : second;
        Account destination = first.getId().equals(destinationId) ? first : second;

        requireOwnedBy(source, userId);
        requireOwnedBy(destination, userId);

        if (source.isCreditCard()) {
            throw new TransferNotAllowedException("A credit card account cannot be the source of a transfer");
        }
        if (!source.isActive() || !destination.isActive()) {
            throw new TransferNotAllowedException("Cannot transfer to or from an inactive account");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new TransferNotAllowedException("Insufficient balance for this transfer");
        }

        source.setBalance(source.getBalance().subtract(amount));

        if (destination.isCreditCard()) {
            BigDecimal newInvoice = destination.getCurrentInvoice().subtract(amount).max(BigDecimal.ZERO);
            destination.setCurrentInvoice(newInvoice);
        } else {
            destination.setBalance(destination.getBalance().add(amount));
        }

        accountRepository.save(source);
        accountRepository.save(destination);

        AccountTransfer transfer = AccountTransfer.builder()
                .user(source.getUser())
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(amount)
                .build();

        AccountTransfer saved = accountTransferRepository.save(transfer);
        return accountMapper.toResponse(saved);
    }

    private Account lockOrThrow(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private void requireOwnedBy(Account account, Long userId) {
        if (!account.getUser().getId().equals(userId)) {
            throw new AccountNotFoundException("Account not found");
        }
    }
}
