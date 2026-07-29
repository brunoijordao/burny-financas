package com.burny.financas.reports.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.burny.financas.reports.dto.CategorySpendingDto;
import com.burny.financas.reports.dto.StatementLineDto;
import com.burny.financas.transactions.entity.TransactionType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class ReportExportersTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    private final StatementPdfExporter statementPdfExporter = new StatementPdfExporter();
    private final StatementExcelExporter statementExcelExporter = new StatementExcelExporter();
    private final CategorySpendingPdfExporter categorySpendingPdfExporter = new CategorySpendingPdfExporter();
    private final CategorySpendingExcelExporter categorySpendingExcelExporter = new CategorySpendingExcelExporter();

    private List<StatementLineDto> statementLines() {
        return List.of(new StatementLineDto(
                START.plusDays(4), "Conta Corrente", "Mercado", TransactionType.EXPENSE, new BigDecimal("150.00"), "Compras"));
    }

    private List<CategorySpendingDto> categorySpending() {
        return List.of(new CategorySpendingDto(1L, "Mercado", "icon", "#111", new BigDecimal("600.00"), new BigDecimal("60.00")));
    }

    @Test
    void statementPdfIsNonEmptyAndParseableWithData() throws IOException {
        byte[] pdf = statementPdfExporter.export(statementLines(), START, END);
        assertParseablePdfContains(pdf, "Mercado");
    }

    @Test
    void statementPdfIsNonEmptyAndParseableWithNoRows() throws IOException {
        byte[] pdf = statementPdfExporter.export(List.of(), START, END);
        assertParseablePdfContains(pdf, "Extrato por Período");
    }

    @Test
    void categorySpendingPdfIsNonEmptyAndParseableWithData() throws IOException {
        byte[] pdf = categorySpendingPdfExporter.export(categorySpending(), START, END);
        assertParseablePdfContains(pdf, "Mercado");
    }

    @Test
    void categorySpendingPdfIsNonEmptyAndParseableWithNoRows() throws IOException {
        byte[] pdf = categorySpendingPdfExporter.export(List.of(), START, END);
        assertParseablePdfContains(pdf, "Gastos por Categoria");
    }

    @Test
    void statementExcelIsNonEmptyAndParseableWithData() throws IOException {
        byte[] xlsx = statementExcelExporter.export(statementLines());
        assertParseableXlsxHasRows(xlsx, 2);
    }

    @Test
    void statementExcelIsNonEmptyAndParseableWithNoRows() throws IOException {
        byte[] xlsx = statementExcelExporter.export(List.of());
        assertParseableXlsxHasRows(xlsx, 1);
    }

    @Test
    void categorySpendingExcelIsNonEmptyAndParseableWithData() throws IOException {
        byte[] xlsx = categorySpendingExcelExporter.export(categorySpending());
        assertParseableXlsxHasRows(xlsx, 2);
    }

    @Test
    void categorySpendingExcelIsNonEmptyAndParseableWithNoRows() throws IOException {
        byte[] xlsx = categorySpendingExcelExporter.export(List.of());
        assertParseableXlsxHasRows(xlsx, 1);
    }

    private void assertParseablePdfContains(byte[] pdf, String expectedText) throws IOException {
        assertThat(pdf).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(0);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains(expectedText);
        }
    }

    private void assertParseableXlsxHasRows(byte[] xlsx, int expectedRowCount) throws IOException {
        assertThat(xlsx).isNotEmpty();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;
            for (Row ignored : sheet) {
                rowCount++;
            }
            assertThat(rowCount).isEqualTo(expectedRowCount);
        }
    }
}
