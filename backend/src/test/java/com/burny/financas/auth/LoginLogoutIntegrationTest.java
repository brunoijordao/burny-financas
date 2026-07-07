package com.burny.financas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginLogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private void registerUser(String email, String password) {
        authService.register(new RegisterRequest(email, password));
    }

    /** /auth/logout is a protected route (deny-by-default) — the caller must be authenticated. */
    private String accessTokenFor(String email) {
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return jwtService.generateAccessToken(userId);
    }

    @Test
    void successfulLoginReturnsAccessAndRefreshTokens() throws Exception {
        registerUser("login-ok@example.com", "Password123");

        String body = objectMapper.writeValueAsString(Map.of("email", "login-ok@example.com", "password", "Password123"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType", org.hamcrest.Matchers.is("Bearer")));
    }

    @Test
    void wrongPasswordReturns401WithGenericMessage() throws Exception {
        registerUser("wrong-pw@example.com", "Password123");

        String body = objectMapper.writeValueAsString(Map.of("email", "wrong-pw@example.com", "password", "WrongPass1"));

        var result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String wrongPasswordMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
        assertThat(wrongPasswordMessage).isEqualTo("Invalid email or password");
    }

    @Test
    void unknownEmailReturns401WithSameGenericMessageAsWrongPassword() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "WhoKnows1"));

        var result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String unknownEmailMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
        assertThat(unknownEmailMessage).isEqualTo("Invalid email or password");
    }

    @Test
    void logoutRevokesRefreshTokenAndIsIdempotent() throws Exception {
        registerUser("logout-user@example.com", "Password123");
        String loginBody = objectMapper.writeValueAsString(Map.of("email", "logout-user@example.com", "password", "Password123"));

        var loginResult = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

        String logoutBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        String accessToken = accessTokenFor("logout-user@example.com");

        // First logout revokes it.
        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(logoutBody))
                .andExpect(status().isOk());

        // Second logout with the same (already revoked) token is still a success (idempotent).
        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(logoutBody))
                .andExpect(status().isOk());

        // The revoked token can no longer be used to refresh.
        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithNonexistentTokenReturns200() throws Exception {
        registerUser("logout-nonexistent@example.com", "Password123");
        String accessToken = accessTokenFor("logout-nonexistent@example.com");
        String logoutBody = objectMapper.writeValueAsString(Map.of("refreshToken", "does-not-exist-at-all"));

        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(logoutBody))
                .andExpect(status().isOk());
    }
}
