package com.burny.financas.auth.service;

import com.burny.financas.auth.entity.RefreshToken;
import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.exception.InvalidRefreshTokenException;
import com.burny.financas.auth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the opaque refresh token lifecycle: issuance, hashed persistence, single-use rotation, and
 * reuse-detection driven cascading revocation. The plain token value is only ever known at issuance
 * time (returned to the caller once); the database only ever stores its SHA-256 hash.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32; // 256 bits of entropy
    private static final long EXPIRATION_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService self;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * {@code self} is a lazily-resolved proxy back to this same bean. It exists solely so that
     * {@link #revokeAllActiveForUser(Long)} can be invoked through the Spring AOP proxy (required
     * for its own {@code REQUIRES_NEW} transaction to actually apply) even when called from another
     * method on this same class — plain {@code this.revokeAllActiveForUser(...)} would bypass the
     * proxy entirely and silently ignore the propagation setting.
     */
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, @Lazy RefreshTokenService self) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.self = self;
    }

    public record IssuedToken(String plainValue, RefreshToken entity) {
    }

    @Transactional
    public IssuedToken issue(User user, String ip) {
        String plain = generatePlainToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(plain))
                .expiresAt(LocalDateTime.now().plusDays(EXPIRATION_DAYS))
                .createdByIp(ip)
                .build();
        entity = refreshTokenRepository.save(entity);
        return new IssuedToken(plain, entity);
    }

    /**
     * Validates the presented refresh token and, if valid, rotates it: the presented token is
     * revoked and a brand-new access-eligible refresh token is issued in its place.
     *
     * <p>If the presented token was already revoked, this is treated as reuse of a stolen/rotated
     * token: every other active refresh token for that user is revoked as a containment measure,
     * and the request is rejected.
     */
    @Transactional
    public IssuedToken rotate(String presentedPlainToken, String ip) {
        RefreshToken existing = findByPlainToken(presentedPlainToken);

        if (existing.isRevoked()) {
            // Runs in its own REQUIRES_NEW transaction (via the self-proxy) so the cascading
            // revocation commits even though this method is about to throw and roll back.
            self.revokeAllActiveForUser(existing.getUser().getId());
            throw new InvalidRefreshTokenException("Refresh token has already been used; all sessions revoked");
        }

        if (existing.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        IssuedToken newToken = issue(existing.getUser(), ip);
        existing.setRevokedAt(LocalDateTime.now());
        existing.setReplacedBy(newToken.entity().getId());
        refreshTokenRepository.save(existing);

        return newToken;
    }

    /**
     * Revokes the presented refresh token. Idempotent: if the token doesn't exist or is already
     * revoked, this silently succeeds (the end state — token unusable — is already satisfied).
     */
    @Transactional
    public void revoke(String presentedPlainToken) {
        refreshTokenRepository.findByTokenHash(hash(presentedPlainToken)).ifPresent(rt -> {
            if (!rt.isRevoked()) {
                rt.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(rt);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllActiveForUser(Long userId) {
        List<RefreshToken> active = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        active.forEach(rt -> rt.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);
    }

    private RefreshToken findByPlainToken(String plainToken) {
        return refreshTokenRepository.findByTokenHash(hash(plainToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
    }

    private String generatePlainToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
