package com.burny.financas.reports.service.export;

import com.burny.financas.reports.dto.CategorySpendingDto;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renders a {@link CategorySpendingDto} list into an `.xlsx` file. Reads only from the DTO produced by {@code ReportService} — no independent data fetching (design.md Risk on formatting duplication). */
@Component
public class CategorySpendingExcelExporter {

    private static final String[] HEADERS = {"Categoria", "Total", "Percentual (%)"};

    public byte[] export(List<CategorySpendingDto> categories) {
        List<Object[]> rows = categories.stream()
                .<Object[]>map(category -> new Object[]{
                        category.categoryName(),
                        category.total(),
                        category.percentage()
                })
                .toList();

        return ExcelTableRenderer.render("Gastos por Categoria", HEADERS, rows);
    }
}
