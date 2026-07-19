package com.burny.financas.auth.ratelimit;

import com.burny.financas.auth.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits {@code POST /pdf-imports} (the multipart upload) to N uploads/hour per authenticated user
 * (Bucket4j, in-memory). Scoped to exactly this path/method so it never applies to
 * {@code POST /pdf-imports/{id}/retry} or any other PDF-import endpoint — retrying a failed import
 * intentionally does not consume this bucket (see specs/api-rate-limiting "Retries do not consume
 * the upload limit"). Runs after {@link com.burny.financas.auth.security.JwtAuthenticationFilter}
 * like {@link UserRateLimitFilter}, since the bucket key is the authenticated user id.
 */
@Component
@RequiredArgsConstructor
public class PdfUploadRateLimitFilter extends OncePerRequestFilter {

    private static final String UPLOAD_PATH = "/pdf-imports";

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isUploadRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(userId, key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            RateLimitResponseWriter.write(response, waitSeconds, objectMapper);
        }
    }

    private boolean isUploadRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && UPLOAD_PATH.equals(request.getRequestURI());
    }

    private Bucket newBucket() {
        RateLimitProperties.Limit limit = rateLimitProperties.getPdfUpload();
        Bandwidth bandwidth = Bandwidth.classic(
                limit.getCapacity(),
                Refill.intervally(limit.getCapacity(), Duration.ofSeconds(limit.getRefillPeriodSeconds()))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
