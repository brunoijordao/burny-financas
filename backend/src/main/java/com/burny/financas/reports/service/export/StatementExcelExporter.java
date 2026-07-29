package com.burny.financas.reports.service.export;

import com.burny.financas.reports.dto.StatementLineDto;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renders a {@link StatementLineDto} list into an `.xlsx` file. Reads only from the DTO produced by {@code ReportService} — no independent data fetching (design.md Risk on formatting duplication). */
@Component
public class StatementExcelExporter {

    private static final String[] HEADERS = {"Data", "Conta", "Categoria", "Tipo", "Descrição", "Valor"};

    public byte[] export(List<StatementLineDto> lines) {
        List<Object[]> rows = lines.stream()
                .<Object[]>map(line -> new Object[]{
                        line.transactionDate(),
                        line.accountName(),
                        line.categoryName(),
                        line.type().name(),
                        line.description(),
                        line.amount()
                })
                .toList();

        return ExcelTableRenderer.render("Extrato", HEADERS, rows);
    }
}
