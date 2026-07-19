package com.burny.financas.pdfimport.exception;

/**
 * Thrown when editing, discarding, or confirming a line item that is not in {@code PENDING} status.
 */
public class PdfImportItemNotEditableException extends RuntimeException {
    public PdfImportItemNotEditableException(String message) {
        super(message);
    }
}
