package com.burny.financas.pdfimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.categories.service.CategoryResolutionService;
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdfInterpretationServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PdfTextExtractionService pdfTextExtractionService;
    @Mock
    private GemmaClient gemmaClient;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryResolutionService categoryResolutionService;

    private PdfInterpretationService service() {
        return new PdfInterpretationService(
                pdfTextExtractionService, gemmaClient, categoryRepository, categoryResolutionService);
    }

    private GemmaCandidateTransaction candidate(String suggestedCategory) {
        return new GemmaCandidateTransaction(
                LocalDate.of(2026, 1, 15), "COMPRA SUPERMERCADO", new BigDecimal("50.00"),
                TransactionType.EXPENSE, suggestedCategory);
    }

    @Test
    void usesModelSuggestedCategoryWhenItMatchesAnOwnedCategory() {
        byte[] pdfBytes = "pdf-bytes".getBytes();
        when(pdfTextExtractionService.extractText(pdfBytes)).thenReturn("extracted text");
        when(gemmaClient.interpret("extracted text")).thenReturn(List.of(candidate("Alimentacao")));

        Category food = Category.builder().id(10L).name("Alimentacao").active(true).build();
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Alimentacao"))
                .thenReturn(Optional.of(food));

        List<ResolvedImportTransaction> result = service().interpret(USER_ID, pdfBytes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(food);
        verifyNoInteractions(categoryResolutionService);
    }

    @Test
    void fallsBackToKeywordResolutionWhenModelSuggestsNoUsableCategory() {
        byte[] pdfBytes = "pdf-bytes".getBytes();
        when(pdfTextExtractionService.extractText(pdfBytes)).thenReturn("extracted text");
        when(gemmaClient.interpret("extracted text")).thenReturn(List.of(candidate(null)));

        Category food = Category.builder().id(10L).name("Alimentacao").active(true).build();
        when(categoryResolutionService.resolve(eq(USER_ID), any())).thenReturn(Optional.of(food));

        List<ResolvedImportTransaction> result = service().interpret(USER_ID, pdfBytes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(food);
    }

    @Test
    void fallsBackToKeywordResolutionWhenSuggestedCategoryMatchesNoOwnedCategory() {
        byte[] pdfBytes = "pdf-bytes".getBytes();
        when(pdfTextExtractionService.extractText(pdfBytes)).thenReturn("extracted text");
        when(gemmaClient.interpret("extracted text")).thenReturn(List.of(candidate("Unknown Category")));
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Unknown Category"))
                .thenReturn(Optional.empty());

        Category food = Category.builder().id(10L).name("Alimentacao").active(true).build();
        when(categoryResolutionService.resolve(eq(USER_ID), any())).thenReturn(Optional.of(food));

        List<ResolvedImportTransaction> result = service().interpret(USER_ID, pdfBytes);

        assertThat(result.get(0).category()).isEqualTo(food);
    }

    @Test
    void leavesTransactionUncategorizedWhenNeitherSourceResolves() {
        byte[] pdfBytes = "pdf-bytes".getBytes();
        when(pdfTextExtractionService.extractText(pdfBytes)).thenReturn("extracted text");
        when(gemmaClient.interpret("extracted text")).thenReturn(List.of(candidate(null)));
        when(categoryResolutionService.resolve(eq(USER_ID), any())).thenReturn(Optional.empty());

        List<ResolvedImportTransaction> result = service().interpret(USER_ID, pdfBytes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isNull();
    }
}
