package com.burny.financas.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Deliberately has no balance/currentInvoice fields: those are never editable directly, only
 * through transfers (see specs/accounts/spec.md "Cannot directly edit account balance or current
 * invoice").
 */
public record UpdateAccountRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Icon is required")
        String icon,

        @NotBlank(message = "Color is required")
        String color,

        @DecimalMin(value = "0.01", message = "Credit limit must be greater than zero")
        BigDecimal creditLimit
) {
}
