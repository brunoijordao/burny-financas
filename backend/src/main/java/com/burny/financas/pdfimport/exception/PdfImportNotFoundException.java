package com.burny.financas.pdfimport.exception;

/**
 * Reused for "not found" across both a PDF import and its line items, mirroring the reuse
 * convention already used by {@code TransactionNotFoundException} for transactions/attachments.
 */
public class PdfImportNotFoundException extends RuntimeException {
    public PdfImportNotFoundException(String message) {
        super(message);
    }
}
