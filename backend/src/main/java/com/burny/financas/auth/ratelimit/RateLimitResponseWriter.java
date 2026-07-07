package com.burny.financas.auth.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

final class RateLimitResponseWriter {

    private RateLimitResponseWriter() {
    }

    static void write(HttpServletResponse response, long retryAfterSeconds, ObjectMapper objectMapper) throws IOException {
        long safeRetryAfter = Math.max(1, retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(safeRetryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded, retry after " + safeRetryAfter + " seconds");

        objectMapper.writeValue(response.getWriter(), body);
    }
}
