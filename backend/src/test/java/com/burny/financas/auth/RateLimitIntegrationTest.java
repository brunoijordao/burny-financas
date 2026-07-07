package com.burny.financas.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Uses small, isolated rate-limit capacities (via {@code @TestPropertySource}, which gets its own
 * Spring context) so the tests run fast without waiting out real 60s windows. Every test uses a
 * distinct fake client IP / distinct user so the singleton in-memory buckets don't bleed state
 * between test methods that share this context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.login.capacity=3",
        "app.rate-limit.login.refill-period-seconds=60",
        "app.rate-limit.general.capacity=3",
        "app.rate-limit.general.refill-period-seconds=60"
})
class RateLimitIntegrationTest {

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

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void loginAttempt(String ip) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "whatever1"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body).with(fromIp(ip)));
    }

    @Test
    void loginUnderLimitIsEvaluatedNormally() throws Exception {
        String ip = "10.1.1.1";
        for (int i = 0; i < 3; i++) {
            // Wrong credentials -> 401, never 429, while under the 3/min cap for this IP.
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "x")))
                            .with(fromIp(ip)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void loginOverLimitReturns429WithRetryAfter() throws Exception {
        String ip = "10.1.1.2";
        for (int i = 0; i < 3; i++) {
            loginAttempt(ip);
        }
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "x")))
                        .with(fromIp(ip)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void loginRateLimitIsScopedPerIpNotGlobal() throws Exception {
        String ipA = "10.1.1.3";
        String ipB = "10.1.1.4";

        for (int i = 0; i < 3; i++) {
            loginAttempt(ipA);
        }
        // ipA is now exhausted...
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "x")))
                        .with(fromIp(ipA)))
                .andExpect(status().isTooManyRequests());

        // ...but ipB is untouched and still gets normal (non-429) handling.
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "x")))
                        .with(fromIp(ipB)))
                .andExpect(status().isUnauthorized());
    }

    private String tokenForNewUser(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return jwtService.generateAccessToken(userId);
    }

    @Test
    void generalEndpointOverLimitReturns429ForThatUser() throws Exception {
        String token = tokenForNewUser("rate-general-1@example.com");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/auth/login-history").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/auth/login-history").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void generalEndpointRateLimitIsScopedPerUserNotGlobal() throws Exception {
        String tokenUserA = tokenForNewUser("rate-general-a@example.com");
        String tokenUserB = tokenForNewUser("rate-general-b@example.com");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/auth/login-history").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                    .andExpect(status().isOk());
        }
        // userA is now exhausted...
        mockMvc.perform(get("/auth/login-history").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isTooManyRequests());

        // ...but userB, a different user, is unaffected.
        mockMvc.perform(get("/auth/login-history").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isOk());
    }
}
