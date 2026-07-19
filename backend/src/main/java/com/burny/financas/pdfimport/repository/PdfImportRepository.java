package com.burny.financas.pdfimport.repository;

import com.burny.financas.pdfimport.entity.PdfImport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PdfImportRepository extends JpaRepository<PdfImport, Long> {

    Optional<PdfImport> findByIdAndUserId(Long id, Long userId);

    List<PdfImport> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
