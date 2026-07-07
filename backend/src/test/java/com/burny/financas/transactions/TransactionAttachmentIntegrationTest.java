package com.burny.financas.transactions;

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
class TransactionAttachmentIntegrationTest {

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

    private long createTransaction(String authHeader, long accountId) throws Exception {
        String response = mockMvc.perform(post("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\",\"amount\":10,\"type\":\"EXPENSE\","
                                + "\"transactionDate\":\"2026-01-15\",\"accountId\":" + accountId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void successfulUploadListDownloadDelete() throws Exception {
        String auth = authHeaderFor("tx-att-full-flow@example.com");
        long accountId = createAccount(auth);
        long txId = createTransaction(auth, accountId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", "fake-png-bytes".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/transactions/" + txId + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename", is("receipt.png")))
                .andExpect(jsonPath("$.contentType", is("image/png")))
                .andReturn().getResponse().getContentAsString();
        long attachmentId = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(get("/transactions/" + txId + "/attachments").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));

        mockMvc.perform(get("/transactions/" + txId + "/attachments/" + attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(content -> {
                    byte[] body = content.getResponse().getContentAsByteArray();
                    org.assertj.core.api.Assertions.assertThat(new String(body)).isEqualTo("fake-png-bytes");
                });

        mockMvc.perform(delete("/transactions/" + txId + "/attachments/" + attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transactions/" + txId + "/attachments").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void unsupportedContentTypeRejectedWith400() throws Exception {
        String auth = authHeaderFor("tx-att-unsupported-type@example.com");
        long accountId = createAccount(auth);
        long txId = createTransaction(auth, accountId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "bytes".getBytes());

        mockMvc.perform(multipart("/transactions/" + txId + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crossUserIsolationOnUploadListDownloadDelete() throws Exception {
        String ownerAuth = authHeaderFor("tx-att-isolation-owner@example.com");
        long accountId = createAccount(ownerAuth);
        long txId = createTransaction(ownerAuth, accountId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.pdf", "application/pdf", "fake-pdf-bytes".getBytes());
        String uploadResponse = mockMvc.perform(multipart("/transactions/" + txId + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long attachmentId = objectMapper.readTree(uploadResponse).get("id").asLong();

        String otherAuth = authHeaderFor("tx-att-isolation-other@example.com");

        MockMultipartFile otherFile = new MockMultipartFile(
                "file", "receipt2.pdf", "application/pdf", "bytes".getBytes());
        mockMvc.perform(multipart("/transactions/" + txId + "/attachments")
                        .file(otherFile)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/transactions/" + txId + "/attachments").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/transactions/" + txId + "/attachments/" + attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/transactions/" + txId + "/attachments/" + attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void attachmentsRemainRetrievableAfterTransactionSoftDeletion() throws Exception {
        String auth = authHeaderFor("tx-att-survive-delete@example.com");
        long accountId = createAccount(auth);
        long txId = createTransaction(auth, accountId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());
        mockMvc.perform(multipart("/transactions/" + txId + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/transactions/" + txId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transactions/" + txId + "/attachments").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }
}
