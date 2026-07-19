package com.burny.financas.pdfimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.pdfimport.entity.PdfImportItemStatus;
import com.burny.financas.pdfimport.entity.PdfImportStatus;
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

/**
 * {@code GemmaClient} is mocked (real network calls are never made in tests) so the AI response is
 * deterministic; PDFBox text extraction runs for real against a minimal valid PDF built in-test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfImportHappyPathIntegrationTest {

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
    void uploadInterpretEditDiscardConfirmAffectsBalance() throws Exception {
        when(gemmaClient.interpret(any())).thenReturn(List.of(
                new GemmaCandidateTransaction(
                        LocalDate.of(2026, 1, 15), "COMPRA SUPERMERCADO", new BigDecimal("50.00"),
                        TransactionType.EXPENSE, null),
                new GemmaCandidateTransaction(
                        LocalDate.of(2026, 1, 16), "ESTORNO", new BigDecimal("30.00"),
                        TransactionType.INCOME, null)
        ));

        String auth = authHeaderFor("pdf-happy-path@example.com");
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

        String detailResponse = mockMvc.perform(get("/pdf-imports/" + importId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfImport.status", is(PdfImportStatus.READY_FOR_REVIEW.name())))
                .andExpect(jsonPath("$.items.length()", is(2)))
                .andReturn().getResponse().getContentAsString();

        var itemsNode = objectMapper.readTree(detailResponse).get("items");
        long expenseItemId = -1;
        long incomeItemId = -1;
        for (var itemNode : itemsNode) {
            if (itemNode.get("type").asText().equals("EXPENSE")) {
                expenseItemId = itemNode.get("id").asLong();
            } else {
                incomeItemId = itemNode.get("id").asLong();
            }
        }
        assertThat(expenseItemId).isPositive();
        assertThat(incomeItemId).isPositive();

        // Edit the expense item's amount from 50.00 to 60.00 before confirming it.
        mockMvc.perform(put("/pdf-imports/" + importId + "/items/" + expenseItemId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionDate\":\"2026-01-15\",\"description\":\"COMPRA SUPERMERCADO\","
                                + "\"amount\":60.00,\"type\":\"EXPENSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(60.00)));

        // Discard the income item entirely.
        mockMvc.perform(delete("/pdf-imports/" + importId + "/items/" + incomeItemId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Confirm the (edited) expense item.
        mockMvc.perform(post("/pdf-imports/" + importId + "/items/" + expenseItemId + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(PdfImportItemStatus.CONFIRMED.name())))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        // Confirming the discarded item must now fail (not pending).
        mockMvc.perform(post("/pdf-imports/" + importId + "/items/" + incomeItemId + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());

        // Only one transaction was created, for the edited (60.00) amount, and the account balance
        // reflects exactly that expense's effect.
        mockMvc.perform(get("/transactions").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].amount", is(60.00)))
                .andExpect(jsonPath("$.content[0].type", is("EXPENSE")));

        mockMvc.perform(get("/accounts/" + accountId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(-60.00)));
    }
}
