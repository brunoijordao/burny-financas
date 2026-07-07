package com.burny.financas.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {
}
