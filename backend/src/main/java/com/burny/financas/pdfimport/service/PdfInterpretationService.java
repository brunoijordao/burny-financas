package com.burny.financas.pdfimport.service;

import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.categories.service.CategoryResolutionService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates extraction (PDFBox) + interpretation (Gemma) + category resolution for one PDF
 * import. Category resolution order per candidate transaction (see specs/pdf-statement-import):
 * 1. Gemma's suggested category name, if it matches one of the user's own active categories.
 * 2. Keyword-based {@link CategoryResolutionService} fallback against the description.
 * 3. Otherwise left uncategorized.
 */
@Service
@RequiredArgsConstructor
public class PdfInterpretationService {

    private final PdfTextExtractionService pdfTextExtractionService;
    private final GemmaClient gemmaClient;
    private final CategoryRepository categoryRepository;
    private final CategoryResolutionService categoryResolutionService;

    public List<ResolvedImportTransaction> interpret(Long userId, byte[] pdfBytes) {
        String statementText = pdfTextExtractionService.extractText(pdfBytes);
        List<GemmaCandidateTransaction> candidates = gemmaClient.interpret(statementText);

        return candidates.stream()
                .map(candidate -> resolve(userId, candidate))
                .toList();
    }

    private ResolvedImportTransaction resolve(Long userId, GemmaCandidateTransaction candidate) {
        Category category = resolveSuggestedCategory(userId, candidate.suggestedCategoryName())
                .or(() -> categoryResolutionService.resolve(userId, candidate.description()))
                .orElse(null);

        return new ResolvedImportTransaction(
                candidate.transactionDate(),
                candidate.description(),
                candidate.amount(),
                candidate.type(),
                category
        );
    }

    private Optional<Category> resolveSuggestedCategory(Long userId, String suggestedCategoryName) {
        if (suggestedCategoryName == null || suggestedCategoryName.isBlank()) {
            return Optional.empty();
        }
        return categoryRepository.findByUserIdAndNameIgnoreCaseAndActiveTrue(userId, suggestedCategoryName.trim());
    }
}
