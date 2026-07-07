package com.burny.financas.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * {@code app.security.trust-forwarded-header} controls whether {@code X-Forwarded-For} is trusted
 * to determine the client IP (e.g. when the app runs behind a known reverse proxy like Nginx). It
 * defaults to false: only the direct connection's remote address is trusted unless a trusted proxy
 * is explicitly configured, since a misconfigured/absent proxy would let a client spoof its IP and
 * dodge rate limiting.
 */
@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    private boolean trustForwardedHeader = false;
}
