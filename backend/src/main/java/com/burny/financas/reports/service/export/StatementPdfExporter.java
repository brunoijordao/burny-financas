package com.burny.financas.reports.service.export;

import com.burny.financas.reports.dto.StatementLineDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renders a {@link StatementLineDto} list into a PDF. Reads only from the DTO produced by {@code ReportService} — no independent data fetching (design.md Risk on formatting duplication). */
@Component
public class StatementPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {"Data", "Conta", "Categoria", "Tipo", "Descrição", "Valor"};
    private static final float[] COLUMN_WIDTHS = {60f, 90f, 90f, 55f, 130f, 90f};

    public byte[] export(List<StatementLineDto> lines, LocalDate startDate, LocalDate endDate) {
        List<String> subtitle = List.of(
                "Período: " + startDate.format(DATE_FORMATTER) + " a " + endDate.format(DATE_FORMATTER));

        List<String[]> rows = lines.stream()
                .map(line -> new String[]{
                        line.transactionDate().format(DATE_FORMATTER),
                        line.accountName(),
                        line.categoryName(),
                        line.type().name(),
                        line.description(),
                        formatAmount(line.amount())
                })
                .toList();

        return PdfTableRenderer.render("Extrato por Período", subtitle, HEADERS, COLUMN_WIDTHS, rows);
    }

    private String formatAmount(BigDecimal amount) {
        return "R$ " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
