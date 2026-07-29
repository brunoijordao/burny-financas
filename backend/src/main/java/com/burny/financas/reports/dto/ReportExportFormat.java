package com.burny.financas.reports.dto;

import com.burny.financas.reports.exception.InvalidReportRequestException;

public enum ReportExportFormat {
    PDF,
    XLSX;

    public static ReportExportFormat fromParam(String value) {
        if (value == null) {
            throw new InvalidReportRequestException("format is required");
        }
        try {
            return ReportExportFormat.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidReportRequestException("Unsupported export format: " + value);
        }
    }
}
