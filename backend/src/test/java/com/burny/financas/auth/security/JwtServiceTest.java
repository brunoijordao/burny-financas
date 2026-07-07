package com.burny.financas.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";

    @Test
    void generatesTokenWithUserIdAsSubjectAndCanParseItBack() {
        JwtService jwtService = new JwtService(SECRET, 900_000L);

        String token = jwtService.generateAccessToken(42L);
        Long userId = jwtService.parseUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void expirationSecondsMatchesConfiguredMs() {
        JwtService jwtService = new JwtService(SECRET, 900_000L);
        assertThat(jwtService.getAccessTokenExpirationSeconds()).isEqualTo(900L);
    }

    @Test
    void expiredTokenFailsParsing() throws InterruptedException {
        // 1ms expiration -> token is expired almost immediately.
        JwtService jwtService = new JwtService(SECRET, 1L);
        String token = jwtService.generateAccessToken(1L);
        Thread.sleep(20);

        assertThatThrownBy(() -> jwtService.parseUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretFailsSignatureValidation() {
        JwtService issuer = new JwtService(SECRET, 900_000L);
        JwtService verifier = new JwtService("a-completely-different-secret-key-of-32-bytes-min", 900_000L);

        String token = issuer.generateAccessToken(7L);

        assertThatThrownBy(() -> verifier.parseUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void malformedTokenFailsParsing() {
        JwtService jwtService = new JwtService(SECRET, 900_000L);

        assertThatThrownBy(() -> jwtService.parseUserId("not-a-valid-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
