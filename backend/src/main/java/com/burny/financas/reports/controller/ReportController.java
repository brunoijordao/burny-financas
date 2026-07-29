package com.burny.financas.reports.controller;

import com.burny.financas.reports.dto.CategorySpendingDto;
import com.burny.financas.reports.dto.NetWorthEvolutionReportDto;
import com.burny.financas.reports.dto.ReportExportFormat;
import com.burny.financas.reports.dto.StatementLineDto;
import com.burny.financas.reports.service.ReportService;
import com.burny.financas.reports.service.export.CategorySpendingExcelExporter;
import com.burny.financas.reports.service.export.CategorySpendingPdfExporter;
import com.burny.financas.reports.service.export.StatementExcelExporter;
import com.burny.financas.reports.service.export.StatementPdfExporter;
import com.burny.financas.transactions.entity.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Read-only reports composed from the authenticated user's own transactions, accounts, and investments; PDF/Excel export of the statement and spending-by-category reports")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final StatementPdfExporter statementPdfExporter;
    private final StatementExcelExporter statementExcelExporter;
    private final CategorySpendingPdfExporter categorySpendingPdfExporter;
    private final CategorySpendingExcelExporter categorySpendingExcelExporter;

    @Operation(summary = "Get the authenticated user's period statement (transaction listing), filterable by account, category, and type")
    @GetMapping("/statement")
    public List<StatementLineDto> statement(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        return reportService.getStatement(currentUserId(authentication), startDate, endDate, accountId, categoryId, type);
    }

    @Operation(summary = "Get the authenticated user's expense totals for a period, grouped by category with each group's percentage of the total")
    @GetMapping("/spending-by-category")
    public List<CategorySpendingDto> spendingByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        return reportService.getSpendingByCategory(currentUserId(authentication), startDate, endDate);
    }

    @Operation(summary = "Get the authenticated user's current consolidated account balance combined with their investment net worth evolution over time")
    @GetMapping("/net-worth-evolution")
    public NetWorthEvolutionReportDto netWorthEvolution(Authentication authentication) {
        return reportService.getNetWorthEvolution(currentUserId(authentication));
    }

    @Operation(summary = "Download the period statement report as a server-generated PDF or Excel file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report file"),
            @ApiResponse(responseCode = "400", description = "Invalid date range or unsupported export format")
    })
    @GetMapping("/statement/export")
    public ResponseEntity<byte[]> exportStatement(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String format,
            Authentication authentication
    ) {
        ReportExportFormat exportFormat = ReportExportFormat.fromParam(format);
        List<StatementLineDto> lines =
                reportService.getStatement(currentUserId(authentication), startDate, endDate, accountId, categoryId, type);

        byte[] file = exportFormat == ReportExportFormat.PDF
                ? statementPdfExporter.export(lines, startDate, endDate)
                : statementExcelExporter.export(lines);

        return download(file, exportFormat, "extrato_" + startDate + "_" + endDate);
    }

    @Operation(summary = "Download the spending-by-category report as a server-generated PDF or Excel file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report file"),
            @ApiResponse(responseCode = "400", description = "Invalid date range or unsupported export format")
    })
    @GetMapping("/spending-by-category/export")
    public ResponseEntity<byte[]> exportSpendingByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String format,
            Authentication authentication
    ) {
        ReportExportFormat exportFormat = ReportExportFormat.fromParam(format);
        List<CategorySpendingDto> categories =
                reportService.getSpendingByCategory(currentUserId(authentication), startDate, endDate);

        byte[] file = exportFormat == ReportExportFormat.PDF
                ? categorySpendingPdfExporter.export(categories, startDate, endDate)
                : categorySpendingExcelExporter.export(categories);

        return download(file, exportFormat, "gastos-por-categoria_" + startDate + "_" + endDate);
    }

    private ResponseEntity<byte[]> download(byte[] file, ReportExportFormat format, String filenameStem) {
        String extension = format == ReportExportFormat.PDF ? ".pdf" : ".xlsx";
        MediaType contentType = format == ReportExportFormat.PDF
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filenameStem + extension + "\"")
                .body(file);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
