package com.burny.financas.settings.dto;

import com.burny.financas.settings.entity.CurrencyCode;
import com.burny.financas.settings.entity.DateFormatCode;

public record UserPreferencesResponse(
        CurrencyCode currency,
        DateFormatCode dateFormat
) {
}
