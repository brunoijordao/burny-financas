package com.burny.financas.investments.dto;

import com.burny.financas.investments.entity.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateInvestmentAssetRequest(
        @NotBlank(message = "Name is required")
        String name,

        String ticker,

        @NotNull(message = "Type is required")
        AssetType type,

        Long accountId
) {
}
