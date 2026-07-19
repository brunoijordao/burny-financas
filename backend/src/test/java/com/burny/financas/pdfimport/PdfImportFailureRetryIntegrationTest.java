package com.burny.financas.pdfimport;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.pdfimport.entity.PdfImportStatus;
import com.burny.financas.pdfimport.exception.PdfInterpretationException;
import com.burny.financas.pdfimport.repository.PdfImportRepository;
import com.burny.financas.pdfimport.service.GemmaCandidateTransaction;
import com.burny.financas.pdfimport.service.GemmaClient;
import com.burny.financas.transactions.entity.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfImportFailureRetryIntegrationTest {

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

    @MockitoBean
    private GemmaClient gemmaClient;

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

    private byte[] realPdfBytes() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("15/01 COMPRA SUPERMERCADO 50,00");
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void gemmaFailureMarksImportFailedPreservesFileAndAllowsRetryWithoutReupload() throws Exception {
        when(gemmaClient.interpret(any()))
                .thenThrow(new PdfInterpretationException("Gemma API call failed: simulated outage"))
                .thenReturn(List.of(new GemmaCandidateTransaction(
                        LocalDate.of(2026, 1, 15), "COMPRA SUPERMERCADO", new BigDecimal("50.00"),
                        TransactionType.EXPENSE, null)));

        String auth = authHeaderFor("pdf-failure-retry@example.com");
        long accountId = createAccount(auth);

        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", realPdfBytes());
        String uploadResponse = mockMvc.perform(multipart("/pdf-imports")
                        .file(file)
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        long importId = objectMapper.readTree(uploadResponse).get("id").asLong();
        String storagePathAfterUpload = pdfImportRepository.findById(importId).orElseThrow().getStoragePath();
        PdfImportTestSupport.awaitProcessingComplete(pdfImportRepository, importId);

        mockMvc.perform(get("/pdf-imports/" + importId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfImport.status", is(PdfImportStatus.FAILED.name())))
                .andExpect(jsonPath("$.pdfImport.failureReason", not(is(""))))
                .andExpect(jsonPath("$.items.length()", is(0)));

        // The originally uploaded file is preserved untouched across the failure.
        String storagePathAfterFailure = pdfImportRepository.findById(importId).orElseThrow().getStoragePath();
        org.assertj.core.api.Assertions.assertThat(storagePathAfterFailure).isEqualTo(storagePathAfterUpload);

        // Retry re-processes the same stored file (no new upload) and this time succeeds.
        mockMvc.perform(post("/pdf-imports/" + importId + "/retry").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
        PdfImportTestSupport.awaitProcessingComplete(pdfImportRepository, importId);

        mockMvc.perform(get("/pdf-imports/" + importId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfImport.status", is(PdfImportStatus.READY_FOR_REVIEW.name())))
                .andExpect(jsonPath("$.pdfImport.failureReason").doesNotExist())
                .andExpect(jsonPath("$.items.length()", is(1)));

        verify(gemmaClient, times(2)).interpret(any());
    }

    @Test
    void retryOnNonFailedImportIsRejected() throws Exception {
        when(gemmaClient.interpret(any())).thenReturn(List.of());

        String auth = authHeaderFor("pdf-retry-not-failed@example.com");
        long accountId = createAccount(auth);

        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", realPdfBytes());
        String uploadResponse = mockMvc.perform(multipart("/pdf-imports")
                        .file(file)
                        .param("accountId", String.valueOf(accountId))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        long importId = objectMapper.readTree(uploadResponse).get("id").asLong();
        PdfImportTestSupport.awaitProcessingComplete(pdfImportRepository, importId);

        mockMvc.perform(get("/pdf-imports/" + importId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.pdfImport.status", is(PdfImportStatus.READY_FOR_REVIEW.name())));

        mockMvc.perform(post("/pdf-imports/" + importId + "/retry").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
    }
}
