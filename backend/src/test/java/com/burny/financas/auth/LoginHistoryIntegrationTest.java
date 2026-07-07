package com.burny.financas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
class LoginHistoryIntegrationTest {

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

    private void attemptLogin(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void userSeesOnlyTheirOwnHistoryMostRecentFirst() throws Exception {
        authService.register(new RegisterRequest("history-user1@example.com", "Password123"));
        authService.register(new RegisterRequest("history-user2@example.com", "Password123"));

        // user1: one failed, then one successful attempt.
        attemptLogin("history-user1@example.com", "WrongPassword1");
        attemptLogin("history-user1@example.com", "Password123");

        // user2: one successful attempt (should never show up in user1's history).
        attemptLogin("history-user2@example.com", "Password123");

        User user1 = userRepository.findByEmail("history-user1@example.com").orElseThrow();
        String accessToken = jwtService.generateAccessToken(user1.getId());

        var result = mockMvc.perform(get("/auth/login-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> entries = objectMapper.readValue(
                result.getResponse().getContentAsString(), List.class);

        assertThat(entries).hasSize(2);
        // Most recent first: the successful attempt was recorded after the failed one.
        assertThat((Boolean) entries.get(0).get("success")).isTrue();
        assertThat((Boolean) entries.get(1).get("success")).isFalse();
        assertThat(entries).allSatisfy(entry ->
                assertThat(entry.get("emailAttempted")).isEqualTo("history-user1@example.com"));
    }

    @Test
    void loginHistoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/login-history"))
                .andExpect(status().isUnauthorized());
    }
}
