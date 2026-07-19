package com.burny.financas.pdfimport.mapper;

import com.burny.financas.pdfimport.dto.PdfImportDetailResponse;
import com.burny.financas.pdfimport.dto.PdfImportItemResponse;
import com.burny.financas.pdfimport.dto.PdfImportResponse;
import com.burny.financas.pdfimport.entity.PdfImport;
import com.burny.financas.pdfimport.entity.PdfImportItem;
import com.burny.financas.pdfimport.service.PdfImportService.PdfImportDetail;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PdfImportMapper {

    default PdfImportResponse toResponse(PdfImport pdfImport) {
        return new PdfImportResponse(
                pdfImport.getId(),
                pdfImport.getAccount().getId(),
                pdfImport.getOriginalFilename(),
                pdfImport.getStatus(),
                pdfImport.getFailureReason(),
                pdfImport.getCreatedAt(),
                pdfImport.getUpdatedAt()
        );
    }

    default PdfImportItemResponse toResponse(PdfImportItem item) {
        return new PdfImportItemResponse(
                item.getId(),
                item.getTransactionDate(),
                item.getDescription(),
                item.getAmount(),
                item.getType(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getStatus(),
                item.getTransaction() != null ? item.getTransaction().getId() : null,
                item.getCreatedAt()
        );
    }

    default PdfImportDetailResponse toDetailResponse(PdfImportDetail detail) {
        List<PdfImportItemResponse> items = detail.items().stream().map(this::toResponse).toList();
        return new PdfImportDetailResponse(toResponse(detail.pdfImport()), items);
    }
}
