package com.burny.financas.pdfimport.dto;

import com.burny.financas.pdfimport.entity.PdfImportStatus;
import java.time.LocalDateTime;

public record PdfImportResponse(
        Long id,
        Long accountId,
        String originalFilename,
        PdfImportStatus status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
