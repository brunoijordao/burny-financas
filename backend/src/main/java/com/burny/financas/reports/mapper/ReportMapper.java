package com.burny.financas.reports.mapper;

import com.burny.financas.reports.dto.StatementLineDto;
import com.burny.financas.transactions.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    String UNCATEGORIZED_LABEL = "Sem categoria";

    default StatementLineDto toStatementLine(Transaction transaction) {
        return new StatementLineDto(
                transaction.getTransactionDate(),
                transaction.getAccount().getName(),
                transaction.getCategory() != null ? transaction.getCategory().getName() : UNCATEGORIZED_LABEL,
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription()
        );
    }
}
