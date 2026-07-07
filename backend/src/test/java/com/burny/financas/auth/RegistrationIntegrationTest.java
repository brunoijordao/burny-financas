package com.burny.financas.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void successfulRegistrationCreatesUserAndReturnsNoPasswordFields() throws Exception {
        String body = objectMapper.writeValueAsString(new Object() {
            public final String email = "new-user@example.com";
            public final String password = "Password123";
        });

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("new-user@example.com")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertPersistedHashIsNotPlainText();
    }

    private void assertPersistedHashIsNotPlainText() {
        var user = userRepository.findByEmail("new-user@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getPasswordHash()).isNotEqualTo("Password123");
        org.assertj.core.api.Assertions.assertThat(user.getPasswordHash()).startsWith("$2");
    }

    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        String body = objectMapper.writeValueAsString(new Object() {
            public final String email = "dup@example.com";
            public final String password = "Password123";
        });

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void weakPasswordIsRejectedWith400() throws Exception {
        String body = objectMapper.writeValueAsString(new Object() {
            public final String email = "weak@example.com";
            public final String password = "short";
        });

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(userRepository.existsByEmail("weak@example.com")).isFalse();
    }

    @Test
    void invalidEmailFormatIsRejectedWith400() throws Exception {
        String body = objectMapper.writeValueAsString(new Object() {
            public final String email = "not-an-email";
            public final String password = "Password123";
        });

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
