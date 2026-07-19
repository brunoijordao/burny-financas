package com.burny.financas.pdfimport;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfImportIsolationIntegrationTest {

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

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private long createAccount(String authHeader) throws Exception {
        String response = mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conta\",\"icon\":\"wallet\",\"color\":\"#000\",\"type\":\"CHECKING\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long uploadImport(String authHeader, long accountId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.pdf", "application/pdf", "not-a-real-pdf".getBytes());
        String response = mockMvc.perform(multipart("/pdf-imports")
                        .file(file)
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void uploadRejectsAccountOwnedByAnotherUser() throws Exception {
        String ownerAuth = authHeaderFor("pdf-iso-owner@example.com");
        long accountId = createAccount(ownerAuth);

        String otherAuth = authHeaderFor("pdf-iso-other@example.com");
        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.pdf", "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/pdf-imports")
                        .file(file)
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersImport() throws Exception {
        String ownerAuth = authHeaderFor("pdf-iso-get-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long importId = uploadImport(ownerAuth, accountId);

        String otherAuth = authHeaderFor("pdf-iso-get-other@example.com");
        mockMvc.perform(get("/pdf-imports/" + importId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnImports() throws Exception {
        String ownerAuth = authHeaderFor("pdf-iso-list-owner@example.com");
        long accountId = createAccount(ownerAuth);
        uploadImport(ownerAuth, accountId);

        String otherAuth = authHeaderFor("pdf-iso-list-other@example.com");
        mockMvc.perform(get("/pdf-imports").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        mockMvc.perform(get("/pdf-imports").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void nonOwnerCannotRetryDiscardOrConfirmAnotherUsersImport() throws Exception {
        String ownerAuth = authHeaderFor("pdf-iso-actions-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long importId = uploadImport(ownerAuth, accountId);

        String otherAuth = authHeaderFor("pdf-iso-actions-other@example.com");

        mockMvc.perform(post("/pdf-imports/" + importId + "/retry").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/pdf-imports/" + importId + "/items/999").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/pdf-imports/" + importId + "/items/999/confirm").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }
}
