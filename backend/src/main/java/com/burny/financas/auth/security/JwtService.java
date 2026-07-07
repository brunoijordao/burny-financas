package com.burny.financas.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Signs and verifies short-lived JWT access tokens (HS256). The signing secret is provided via
 * the {@code app.jwt.secret} property (bound to the JWT_SECRET env var); the token never contains
 * anything but the user id ({@code sub}) and standard time claims, and is never persisted.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * @return the access token lifetime in seconds, for the {@code expiresIn} field in auth responses.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    /**
     * Parses and validates the token (signature + expiration). Throws
     * {@link io.jsonwebtoken.JwtException} (or a subclass, e.g. {@link io.jsonwebtoken.ExpiredJwtException})
     * if the token is malformed, has an invalid signature, or is expired.
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
