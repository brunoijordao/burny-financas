package com.burny.financas.pdfimport.dto;

import com.burny.financas.pdfimport.entity.PdfImportItemStatus;
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PdfImportItemResponse(
        Long id,
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        Long categoryId,
        PdfImportItemStatus status,
        Long transactionId,
        LocalDateTime createdAt
) {
}
