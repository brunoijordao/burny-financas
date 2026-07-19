package com.burny.financas.pdfimport.dto;

import java.util.List;

public record PdfImportDetailResponse(
        PdfImportResponse pdfImport,
        List<PdfImportItemResponse> items
) {
}
