package com.burny.financas.pdfimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.burny.financas.pdfimport.exception.PdfInterpretationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService service = new PdfTextExtractionService();

    private byte[] pdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void extractsTextFromValidPdf() throws IOException {
        byte[] pdfBytes = pdfWithText("15/01 COMPRA SUPERMERCADO 123,45");

        String extracted = service.extractText(pdfBytes);

        assertThat(extracted).contains("COMPRA SUPERMERCADO");
    }

    @Test
    void corruptPdfThrowsPdfInterpretationException() {
        byte[] garbage = "not a real pdf file".getBytes();

        assertThatThrownBy(() -> service.extractText(garbage))
                .isInstanceOf(PdfInterpretationException.class);
    }
}
