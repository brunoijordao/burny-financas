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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits {@code POST /auth/login} to N requests/minute per source IP (Bucket4j, in-memory).
 * Runs before the JWT filter, since login is public/unauthenticated. On rejection it short-circuits
 * the filter chain entirely so the request never reaches credential verification, and the attempt
 * is therefore never written to login history.
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";

    private final RateLimitProperties rateLimitProperties;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIpResolver.resolve(request);
        Bucket bucket = buckets.computeIfAbsent(ip, key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
            RateLimitResponseWriter.write(response, waitSeconds, objectMapper);
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI());
    }

    private Bucket newBucket() {
        RateLimitProperties.Limit limit = rateLimitProperties.getLogin();
        Bandwidth bandwidth = Bandwidth.classic(
                limit.getCapacity(),
                Refill.intervally(limit.getCapacity(), Duration.ofSeconds(limit.getRefillPeriodSeconds()))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
