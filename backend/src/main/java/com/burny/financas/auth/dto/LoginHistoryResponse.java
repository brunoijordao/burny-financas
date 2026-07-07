package com.burny.financas.auth.dto;

import java.time.LocalDateTime;

public record LoginHistoryResponse(
        Long id,
        String emailAttempted,
        String ipAddress,
        boolean success,
        LocalDateTime createdAt
) {
}
