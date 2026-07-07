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
 * Limits authenticated requests to N requests/minute per user id (Bucket4j, in-memory). Runs after
 * {@link com.burny.financas.auth.security.JwtAuthenticationFilter}, so it only acts when the
 * request is already authenticated; unauthenticated requests (public routes, or protected routes
 * without a valid token) pass through untouched — the latter are rejected downstream by Spring
 * Security's authorization rules instead. Login is excluded here since it has its own IP-based
 * limiter and is never authenticated at this point.
 */
@Component
@RequiredArgsConstructor
public class UserRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
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

    private Bucket newBucket() {
        RateLimitProperties.Limit limit = rateLimitProperties.getGeneral();
        Bandwidth bandwidth = Bandwidth.classic(
                limit.getCapacity(),
                Refill.intervally(limit.getCapacity(), Duration.ofSeconds(limit.getRefillPeriodSeconds()))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
