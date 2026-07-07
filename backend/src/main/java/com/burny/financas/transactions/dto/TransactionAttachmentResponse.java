package com.burny.financas.transactions.dto;

import java.time.LocalDateTime;

public record TransactionAttachmentResponse(
        Long id,
        Long transactionId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        LocalDateTime createdAt
) {
}
