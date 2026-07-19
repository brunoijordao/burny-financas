package com.burny.financas.pdfimport;

import com.burny.financas.pdfimport.entity.PdfImportStatus;
import com.burny.financas.pdfimport.repository.PdfImportRepository;

/**
 * Processing now always runs on a genuine background thread (see {@code AsyncConfig}'s javadoc for
 * why a same-thread test executor was tried and reverted), so tests must wait for it instead of
 * assuming it completed synchronously by the time the upload/retry HTTP call returns.
 */
final class PdfImportTestSupport {

    private PdfImportTestSupport() {
    }

    static PdfImportStatus awaitProcessingComplete(PdfImportRepository repository, Long importId) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            PdfImportStatus status = repository.findById(importId).orElseThrow().getStatus();
            if (status != PdfImportStatus.PROCESSING) {
                return status;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("PDF import " + importId + " did not finish processing within 5s");
    }
}
