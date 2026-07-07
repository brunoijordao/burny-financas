package com.burny.financas.accounts.mapper;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.dto.TransferResponse;
import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountTransfer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    /**
     * A CREDIT_CARD account reports {@code creditLimit}/{@code currentInvoice} instead of a
     * spendable {@code balance}; every other type is the reverse (see "Individual Account Balance").
     */
    default AccountResponse toResponse(Account account) {
        boolean creditCard = account.isCreditCard();
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getIcon(),
                account.getColor(),
                account.getType(),
                account.isActive(),
                creditCard ? null : account.getBalance(),
                creditCard ? account.getCreditLimit() : null,
                creditCard ? account.getCurrentInvoice() : null,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    default TransferResponse toResponse(AccountTransfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(),
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }
}
