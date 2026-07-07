package com.burny.financas.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionBalanceServiceTest {

    private final TransactionBalanceService service = new TransactionBalanceService();

    private Account checkingAccount(String balance) {
        return Account.builder()
                .type(AccountType.CHECKING)
                .balance(new BigDecimal(balance))
                .build();
    }

    private Account creditCardAccount(String currentInvoice) {
        return Account.builder()
                .type(AccountType.CREDIT_CARD)
                .currentInvoice(new BigDecimal(currentInvoice))
                .build();
    }

    @Test
    void expenseOnCheckingDebitsBalance() {
        Account account = checkingAccount("100.00");
        service.applyEffect(account, TransactionType.EXPENSE, new BigDecimal("30.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void expenseOnCreditCardIncreasesCurrentInvoice() {
        Account account = creditCardAccount("50.00");
        service.applyEffect(account, TransactionType.EXPENSE, new BigDecimal("20.00"));
        assertThat(account.getCurrentInvoice()).isEqualByComparingTo("70.00");
    }

    @Test
    void incomeOnCheckingCreditsBalance() {
        Account account = checkingAccount("100.00");
        service.applyEffect(account, TransactionType.INCOME, new BigDecimal("40.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("140.00");
    }

    @Test
    void incomeOnCreditCardReducesCurrentInvoice() {
        Account account = creditCardAccount("100.00");
        service.applyEffect(account, TransactionType.INCOME, new BigDecimal("30.00"));
        assertThat(account.getCurrentInvoice()).isEqualByComparingTo("70.00");
    }

    @Test
    void incomeOnCreditCardFloorsAtZeroOnOverpayment() {
        Account account = creditCardAccount("50.00");
        service.applyEffect(account, TransactionType.INCOME, new BigDecimal("200.00"));
        assertThat(account.getCurrentInvoice()).isEqualByComparingTo("0.00");
    }

    @Test
    void reversingExpenseOnCheckingRestoresBalance() {
        Account account = checkingAccount("70.00");
        service.reverseEffect(account, TransactionType.EXPENSE, new BigDecimal("30.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void reversingIncomeOnCheckingRestoresBalance() {
        Account account = checkingAccount("140.00");
        service.reverseEffect(account, TransactionType.INCOME, new BigDecimal("40.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void reversingExpenseOnCreditCardRestoresCurrentInvoice() {
        Account account = creditCardAccount("70.00");
        service.reverseEffect(account, TransactionType.EXPENSE, new BigDecimal("20.00"));
        assertThat(account.getCurrentInvoice()).isEqualByComparingTo("50.00");
    }

    @Test
    void reversingIncomeOnCreditCardRestoresCurrentInvoiceWhenNotFloored() {
        Account account = creditCardAccount("70.00");
        service.reverseEffect(account, TransactionType.INCOME, new BigDecimal("30.00"));
        assertThat(account.getCurrentInvoice()).isEqualByComparingTo("100.00");
    }
}
