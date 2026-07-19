package com.burny.financas.pdfimport.exception;

/**
 * Raised for any failure in the extraction/interpretation pipeline (unreadable PDF, Gemma
 * unavailable, Gemma error response, malformed/unparseable JSON, timeout). Always caught by
 * {@code PdfImportService}'s async processing and turned into a {@code FAILED} import with a
 * user-facing reason — never allowed to propagate as an unhandled server error.
 */
public class PdfInterpretationException extends RuntimeException {
    public PdfInterpretationException(String message) {
        super(message);
    }

    public PdfInterpretationException(String message, Throwable cause) {
        super(message, cause);
    }
}
