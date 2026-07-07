package com.burny.financas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the route-protection spec: deny-by-default on any non-allowlisted route, and the public
 * allowlist (register/login/refresh) reachable without a token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RouteProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private static final String PROTECTED_ROUTE = "/auth/login-history";

    @Test
    void missingAuthorizationHeaderOnProtectedRouteIsRejected() throws Exception {
        mockMvc.perform(get(PROTECTED_ROUTE)).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedTokenOnProtectedRouteIsRejected() throws Exception {
        mockMvc.perform(get(PROTECTED_ROUTE).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithBadSignatureOnProtectedRouteIsRejected() throws Exception {
        JwtService otherIssuer = new JwtService("a-totally-different-signing-key-of-32-bytes-min", 900_000L);
        String foreignToken = otherIssuer.generateAccessToken(1L);

        mockMvc.perform(get(PROTECTED_ROUTE).header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenOnProtectedRouteIsRejected() throws Exception {
        JwtService shortLived = new JwtService(jwtSecret, 1L);
        String expiredToken = shortLived.generateAccessToken(1L);
        Thread.sleep(20);

        mockMvc.perform(get(PROTECTED_ROUTE).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenOnProtectedRouteIsAccepted() throws Exception {
        authService.register(new RegisterRequest("route-protection@example.com", "Password123"));
        Long userId = userRepository.findByEmail("route-protection@example.com").orElseThrow().getId();
        String accessToken = jwtService.generateAccessToken(userId);

        mockMvc.perform(get(PROTECTED_ROUTE).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void publicRegisterRouteIsReachableWithoutToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "public-register@example.com", "password", "Password123"));
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void publicLoginRouteIsReachableWithoutToken() throws Exception {
        authService.register(new RegisterRequest("public-login@example.com", "Password123"));
        String body = objectMapper.writeValueAsString(Map.of("email", "public-login@example.com", "password", "Password123"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void publicRefreshRouteIsReachableWithoutTokenAndAppliesBusinessLogic() throws Exception {
        // No Authorization header at all -> the security filter must let it through to the
        // handler, which then rejects it for business reasons (unknown token), not because the
        // filter blocked it.
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", "totally-unknown-token"));
        var result = mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String message = objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
        assertThat(message).isNotEqualTo("Authentication required");
    }
}
