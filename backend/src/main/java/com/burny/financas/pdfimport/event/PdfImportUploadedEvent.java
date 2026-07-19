package com.burny.financas.pdfimport.event;

/**
 * Published after a PDF import's row (upload or retry) has committed, to trigger the async
 * extraction+interpretation pipeline. Using an event instead of a direct cross-service call avoids a
 * circular dependency between {@code PdfImportService} and its background processor, and — combined
 * with {@code @TransactionalEventListener(phase = AFTER_COMMIT)} on the listener — guarantees the
 * background thread never reads the import row before the row exists.
 */
public record PdfImportUploadedEvent(Long importId, Long userId) {
}
