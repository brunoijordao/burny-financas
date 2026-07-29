package com.burny.financas.reports.dto;

import com.burny.financas.reports.exception.InvalidReportRequestException;
import java.time.LocalDate;

/**
 * Validated start/end date pair shared by the period-scoped reports. Validation lives here
 * (invoked from the service layer, never the controller) per design.md Decision 6 — an inverted
 * range is a business rule, not a shape check.
 */
public record ReportPeriod(LocalDate startDate, LocalDate endDate) {

    public static ReportPeriod of(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidReportRequestException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidReportRequestException("startDate must not be after endDate");
        }
        return new ReportPeriod(startDate, endDate);
    }
}
