package com.burny.financas.settings.dto;

import com.burny.financas.settings.entity.CurrencyCode;
import com.burny.financas.settings.entity.DateFormatCode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferencesRequest(
        @NotNull(message = "Currency is required")
        CurrencyCode currency,

        @NotNull(message = "Date format is required")
        DateFormatCode dateFormat
) {
}
