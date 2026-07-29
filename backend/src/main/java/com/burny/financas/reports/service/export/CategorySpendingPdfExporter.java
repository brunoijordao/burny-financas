package com.burny.financas.reports.service.export;

import com.burny.financas.reports.dto.CategorySpendingDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renders a {@link CategorySpendingDto} list into a PDF. Reads only from the DTO produced by {@code ReportService} — no independent data fetching (design.md Risk on formatting duplication). */
@Component
public class CategorySpendingPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {"Categoria", "Total", "Percentual"};
    private static final float[] COLUMN_WIDTHS = {220f, 130f, 90f};

    public byte[] export(List<CategorySpendingDto> categories, LocalDate startDate, LocalDate endDate) {
        List<String> subtitle = List.of(
                "Período: " + startDate.format(DATE_FORMATTER) + " a " + endDate.format(DATE_FORMATTER));

        List<String[]> rows = categories.stream()
                .map(category -> new String[]{
                        category.categoryName(),
                        formatAmount(category.total()),
                        formatPercentage(category.percentage())
                })
                .toList();

        return PdfTableRenderer.render("Gastos por Categoria", subtitle, HEADERS, COLUMN_WIDTHS, rows);
    }

    private String formatAmount(BigDecimal amount) {
        return "R$ " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercentage(BigDecimal percentage) {
        return percentage.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
