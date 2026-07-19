package com.burny.financas.pdfimport.repository;

import com.burny.financas.pdfimport.entity.PdfImportItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PdfImportItemRepository extends JpaRepository<PdfImportItem, Long> {

    Optional<PdfImportItem> findByIdAndPdfImportId(Long id, Long pdfImportId);

    List<PdfImportItem> findAllByPdfImportIdOrderByTransactionDateDesc(Long pdfImportId);
}
