package com.burny.financas.pdfimport.service;

import com.burny.financas.pdfimport.exception.PdfInterpretationException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

/**
 * Extracts text server-side only (see specs/pdf-statement-import "Backend-Only PDF Text
 * Extraction"). The extracted text is returned to the caller in memory and is never persisted or
 * returned through any API response (design.md "Decision 10").
 */
@Service
public class PdfTextExtractionService {

    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new PdfInterpretationException("Could not read the uploaded PDF file", e);
        }
    }
}
