package com.burny.financas.settings;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
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
class UserPreferencesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    @Test
    void defaultsAreReturnedWhenNothingWasEverSaved() throws Exception {
        String auth = authHeaderFor("preferences-defaults@example.com");

        mockMvc.perform(get("/settings/preferences").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("BRL")))
                .andExpect(jsonPath("$.dateFormat", is("DD/MM/YYYY")));
    }

    @Test
    void firstUpdateCreatesThePreferenceRecord() throws Exception {
        String auth = authHeaderFor("preferences-create@example.com");

        mockMvc.perform(put("/settings/preferences")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\",\"dateFormat\":\"MM/DD/YYYY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.dateFormat", is("MM/DD/YYYY")));

        mockMvc.perform(get("/settings/preferences").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.dateFormat", is("MM/DD/YYYY")));
    }

    @Test
    void subsequentUpdateOverwritesTheExistingRecordInsteadOfCreatingASecondOne() throws Exception {
        String auth = authHeaderFor("preferences-update@example.com");

        mockMvc.perform(put("/settings/preferences")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\",\"dateFormat\":\"MM/DD/YYYY\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/settings/preferences")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\",\"dateFormat\":\"YYYY-MM-DD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.dateFormat", is("YYYY-MM-DD")));

        mockMvc.perform(get("/settings/preferences").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.dateFormat", is("YYYY-MM-DD")));
    }

    @Test
    void preferencesAreIsolatedPerUser() throws Exception {
        String firstAuth = authHeaderFor("preferences-isolation-1@example.com");
        mockMvc.perform(put("/settings/preferences")
                        .header(HttpHeaders.AUTHORIZATION, firstAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\",\"dateFormat\":\"MM/DD/YYYY\"}"))
                .andExpect(status().isOk());

        String secondAuth = authHeaderFor("preferences-isolation-2@example.com");

        mockMvc.perform(get("/settings/preferences").header(HttpHeaders.AUTHORIZATION, secondAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("BRL")))
                .andExpect(jsonPath("$.dateFormat", is("DD/MM/YYYY")));
    }

    @Test
    void unsupportedCurrencyIsRejectedWith400() throws Exception {
        String auth = authHeaderFor("preferences-invalid-currency@example.com");

        mockMvc.perform(put("/settings/preferences")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"GBP\",\"dateFormat\":\"DD/MM/YYYY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedDateFormatIsRejectedWith400() throws Exception {
        String auth = authHeaderFor("preferences-invalid-date-format@example.com");

        mockMvc.perform(put("/settings/preferences")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"BRL\",\"dateFormat\":\"YYYY/MM/DD\"}"))
                .andExpect(status().isBadRequest());
    }
}
