package com.burny.financas.reports.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Shared low-level table layout for the report PDFs (design.md Decision 3: PDFBox's
 * content-stream API, reused rather than adding iText). Both {@link StatementPdfExporter} and
 * {@link CategorySpendingPdfExporter} only build headers/rows/column widths and hand them here —
 * page creation and pagination live in exactly one place.
 */
final class PdfTableRenderer {

    private static final float MARGIN = 40f;
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float SUBTITLE_FONT_SIZE = 10f;
    private static final float HEADER_FONT_SIZE = 10f;
    private static final float ROW_FONT_SIZE = 9f;
    private static final float ROW_HEIGHT = 16f;

    private PdfTableRenderer() {
    }

    static byte[] render(String title, List<String> subtitleLines, String[] headers, float[] columnWidths, List<String[]> rows) {
        try (PDDocument document = new PDDocument()) {
            PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PageCursor cursor = new PageCursor(document, boldFont);
            cursor.newPage();
            cursor.writeText(boldFont, TITLE_FONT_SIZE, title);
            cursor.advance(TITLE_FONT_SIZE + 6);
            for (String subtitle : subtitleLines) {
                cursor.writeText(regularFont, SUBTITLE_FONT_SIZE, subtitle);
                cursor.advance(SUBTITLE_FONT_SIZE + 4);
            }
            cursor.advance(10);

            cursor.writeRow(boldFont, HEADER_FONT_SIZE, headers, columnWidths);
            cursor.advance(ROW_HEIGHT);

            for (String[] row : rows) {
                if (cursor.needsNewPage()) {
                    cursor.newPage();
                    cursor.writeRow(boldFont, HEADER_FONT_SIZE, headers, columnWidths);
                    cursor.advance(ROW_HEIGHT);
                }
                cursor.writeRow(regularFont, ROW_FONT_SIZE, row, columnWidths);
                cursor.advance(ROW_HEIGHT);
            }
            cursor.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate report PDF", ex);
        }
    }

    /** Tracks the current page/content-stream and vertical write position, creating a new page on overflow. */
    private static final class PageCursor {
        private final PDDocument document;
        private final PDFont headerFont;
        private PDPageContentStream contentStream;
        private float y;

        PageCursor(PDDocument document, PDFont headerFont) {
            this.document = document;
            this.headerFont = headerFont;
        }

        void newPage() throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        boolean needsNewPage() {
            return y - ROW_HEIGHT < MARGIN;
        }

        void advance(float amount) {
            y -= amount;
        }

        void writeText(PDFont font, float fontSize, String text) throws IOException {
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(sanitize(text));
            contentStream.endText();
        }

        void writeRow(PDFont font, float fontSize, String[] cells, float[] columnWidths) throws IOException {
            float x = MARGIN;
            for (int i = 0; i < cells.length; i++) {
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(x, y);
                contentStream.showText(sanitize(truncate(font, fontSize, cells[i], columnWidths[i])));
                contentStream.endText();
                x += columnWidths[i];
            }
        }

        void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private String truncate(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
            if (text == null) {
                return "";
            }
            String candidate = text;
            while (!candidate.isEmpty() && font.getStringWidth(candidate) / 1000f * fontSize > maxWidth) {
                candidate = candidate.substring(0, candidate.length() - 1);
            }
            return candidate;
        }

        /** PDFBox's WinAnsiEncoding can't render every Unicode codepoint (e.g. some emoji used as category icons). */
        private String sanitize(String text) {
            return text == null ? "" : text.replaceAll("[^\\x00-\\xFF]", "?");
        }
    }
}
