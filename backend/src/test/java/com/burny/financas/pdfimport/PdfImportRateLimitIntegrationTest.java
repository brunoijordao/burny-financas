package com.burny.financas.pdfimport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.pdfimport.repository.PdfImportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Own {@code @TestPropertySource} (own Spring context, per the same convention as
 * {@code RateLimitIntegrationTest}) with a small PDF-upload capacity so the test doesn't need to
 * wait out the real hourly window.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.pdf-upload.capacity=3",
        "app.rate-limit.pdf-upload.refill-period-seconds=3600"
})
class PdfImportRateLimitIntegrationTest {

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

    @Autowired
    private PdfImportRepository pdfImportRepository;

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

    private MvcResult upload(String authHeader, long accountId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.pdf", "application/pdf", "not-a-real-pdf".getBytes());
        return mockMvc.perform(multipart("/pdf-imports")
                        .file(file)
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andReturn();
    }

    @Test
    void uploadOverLimitReturns429WithRetryAfter() throws Exception {
        String auth = authHeaderFor("pdf-rate-over-limit@example.com");
        long accountId = createAccount(auth);

        for (int i = 0; i < 3; i++) {
            upload(auth, accountId);
        }

        mockMvc.perform(multipart("/pdf-imports")
                        .file(new MockMultipartFile("file", "extrato.pdf", "application/pdf", "x".getBytes()))
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void uploadRateLimitIsScopedPerUserNotGlobal() throws Exception {
        String authA = authHeaderFor("pdf-rate-user-a@example.com");
        long accountA = createAccount(authA);
        for (int i = 0; i < 3; i++) {
            upload(authA, accountA);
        }
        mockMvc.perform(multipart("/pdf-imports")
                        .file(new MockMultipartFile("file", "extrato.pdf", "application/pdf", "x".getBytes()))
                        .param("accountId", String.valueOf(accountA))
                        .header(HttpHeaders.AUTHORIZATION, authA))
                .andExpect(status().isTooManyRequests());

        String authB = authHeaderFor("pdf-rate-user-b@example.com");
        long accountB = createAccount(authB);
        mockMvc.perform(multipart("/pdf-imports")
                        .file(new MockMultipartFile("file", "extrato.pdf", "application/pdf", "x".getBytes()))
                        .param("accountId", String.valueOf(accountB))
                        .header(HttpHeaders.AUTHORIZATION, authB))
                .andExpect(status().isAccepted());
    }

    @Test
    void retryDoesNotConsumeTheUploadRateLimitBucket() throws Exception {
        String auth = authHeaderFor("pdf-rate-retry-exempt@example.com");
        long accountId = createAccount(auth);

        long lastImportId = -1;
        for (int i = 0; i < 3; i++) {
            MvcResult result = upload(auth, accountId);
            lastImportId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        }

        // Upload bucket is now exhausted for this user...
        mockMvc.perform(multipart("/pdf-imports")
                        .file(new MockMultipartFile("file", "extrato.pdf", "application/pdf", "x".getBytes()))
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isTooManyRequests());

        // ...but retrying one of the (invalid-PDF-bytes, therefore FAILED) prior imports is unaffected.
        PdfImportTestSupport.awaitProcessingComplete(pdfImportRepository, lastImportId);
        mockMvc.perform(post("/pdf-imports/" + lastImportId + "/retry").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
    }
}
