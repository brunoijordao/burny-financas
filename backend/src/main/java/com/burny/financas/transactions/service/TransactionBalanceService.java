package com.burny.financas.transactions.service;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Mutates an already-locked {@link Account} entity in memory to reflect a transaction's effect;
 * persisting the account is the caller's responsibility (mirrors {@code AccountTransferService},
 * which also mutates locked entities before an explicit {@code save}).
 *
 * <p>{@code reverseEffect} is implemented as applying the opposite {@link TransactionType}'s effect
 * for the same amount. This is exact in every case except one: reversing an {@code INCOME}
 * transaction on a {@code CREDIT_CARD} account whose original application had been floored at zero
 * (overpayment). In that narrow case the reversal cannot perfectly restore the pre-transaction
 * invoice, because the floor already discarded how much of the amount was "used" — the same
 * information loss already accepted for transfers in {@code account-transfers}' design (see its
 * "Overpayment does not push the invoice negative" scenario). Editing/deleting an overpaying income
 * transaction on a credit card is a rare edge case; this trade-off is accepted rather than adding a
 * pre-effect snapshot column to precisely undo it.
 */
@Component
public class TransactionBalanceService {

    public void applyEffect(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.EXPENSE) {
            if (account.isCreditCard()) {
                account.setCurrentInvoice(account.getCurrentInvoice().add(amount));
            } else {
                account.setBalance(account.getBalance().subtract(amount));
            }
        } else {
            if (account.isCreditCard()) {
                BigDecimal newInvoice = account.getCurrentInvoice().subtract(amount).max(BigDecimal.ZERO);
                account.setCurrentInvoice(newInvoice);
            } else {
                account.setBalance(account.getBalance().add(amount));
            }
        }
    }

    public void reverseEffect(Account account, TransactionType type, BigDecimal amount) {
        TransactionType opposite = type == TransactionType.EXPENSE ? TransactionType.INCOME : TransactionType.EXPENSE;
        applyEffect(account, opposite, amount);
    }
}
