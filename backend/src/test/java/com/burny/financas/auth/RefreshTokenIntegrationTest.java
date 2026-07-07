package com.burny.financas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.entity.RefreshToken;
import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.repository.RefreshTokenRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private String login(String email) throws Exception {
        authService.register(new RegisterRequest(email, "Password123"));
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "Password123"));
        var result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("refreshToken").asText();
    }

    private String hash(String plain) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void successfulRefreshRotatesTokenAndOldOneStopsWorking() throws Exception {
        String refreshToken = login("refresh-ok@example.com");

        var result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // The old (now revoked) token must be rejected if presented again.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        String refreshToken = login("refresh-expired@example.com");

        RefreshToken entity = refreshTokenRepository.findByTokenHash(hash(refreshToken)).orElseThrow();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        refreshTokenRepository.save(entity);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reuseOfRevokedTokenCascadesRevocationToAllActiveTokensForUser() throws Exception {
        String firstToken = login("reuse-detect@example.com");
        User user = userRepository.findByEmail("reuse-detect@example.com").orElseThrow();

        // Rotate once: firstToken becomes revoked, secondToken is the new active one.
        var rotateResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstToken))))
                .andExpect(status().isOk())
                .andReturn();
        String secondToken = objectMapper.readTree(rotateResult.getResponse().getContentAsString()).get("refreshToken").asText();

        // Present the already-revoked firstToken again -> reuse detected.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstToken))))
                .andExpect(status().isUnauthorized());

        // The entire active family for this user, including secondToken, must now be revoked.
        List<RefreshToken> remainingActive = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        assertThat(remainingActive).isEmpty();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", secondToken))))
                .andExpect(status().isUnauthorized());
    }
}
