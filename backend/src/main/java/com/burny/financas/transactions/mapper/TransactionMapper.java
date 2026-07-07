package com.burny.financas.transactions.mapper;

import com.burny.financas.transactions.dto.TransactionResponse;
import com.burny.financas.transactions.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    default TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTransactionDate(),
                transaction.getAccount().getId(),
                transaction.getCategory() != null ? transaction.getCategory().getId() : null,
                transaction.getNote(),
                transaction.getRecurrence() != null ? transaction.getRecurrence().getId() : null,
                transaction.isActive(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
