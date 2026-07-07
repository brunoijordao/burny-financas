package com.burny.financas.accounts.dto;

import com.burny.financas.accounts.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Icon is required")
        String icon,

        @NotBlank(message = "Color is required")
        String color,

        @NotNull(message = "Type is required")
        AccountType type,

        @DecimalMin(value = "0.01", message = "Credit limit must be greater than zero")
        BigDecimal creditLimit
) {
}
