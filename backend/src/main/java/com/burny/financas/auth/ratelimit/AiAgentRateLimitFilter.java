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
 * Limits {@code POST /ai-agent/messages} (the only endpoint that calls Gemini) to N messages/hour
 * per authenticated user (Bucket4j, in-memory), mirroring {@link PdfUploadRateLimitFilter}. Scoped
 * to exactly this path/method so it never applies to {@code POST /ai-agent/transactions/confirm} —
 * confirming a draft never calls Gemini and is only subject to the existing general limit (see
 * specs/api-rate-limiting "Confirming a draft transaction does not consume the chat limit").
 */
@Component
@RequiredArgsConstructor
public class AiAgentRateLimitFilter extends OncePerRequestFilter {

    private static final String MESSAGES_PATH = "/ai-agent/messages";

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isMessagesRequest(request)) {
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

    private boolean isMessagesRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && MESSAGES_PATH.equals(request.getRequestURI());
    }

    private Bucket newBucket() {
        RateLimitProperties.Limit limit = rateLimitProperties.getAiAgent();
        Bandwidth bandwidth = Bandwidth.classic(
                limit.getCapacity(),
                Refill.intervally(limit.getCapacity(), Duration.ofSeconds(limit.getRefillPeriodSeconds()))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
