package com.burny.financas.pdfimport.listener;

import com.burny.financas.pdfimport.entity.PdfImport;
import com.burny.financas.pdfimport.event.PdfImportUploadedEvent;
import com.burny.financas.pdfimport.exception.PdfImportNotFoundException;
import com.burny.financas.pdfimport.exception.PdfInterpretationException;
import com.burny.financas.pdfimport.repository.PdfImportRepository;
import com.burny.financas.pdfimport.service.PdfImportService;
import com.burny.financas.pdfimport.service.PdfInterpretationService;
import com.burny.financas.pdfimport.service.PdfStorageService;
import com.burny.financas.pdfimport.service.ResolvedImportTransaction;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs PDF extraction + Gemma interpretation off the request thread (see design.md "Decision 1").
 * {@code @TransactionalEventListener(AFTER_COMMIT)} ensures this never runs before the
 * upload/retry transaction that created/reset the {@link PdfImport} row has actually committed;
 * {@code @Async} keeps it off both the request thread and the transaction-commit thread. Any failure
 * here is turned into a {@code FAILED} import with a user-facing reason (see specs/pdf-statement-import
 * "Interpretation Failure Handling With Retry Without Re-upload") rather than propagated, since there
 * is no caller left to receive an exception from at this point.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfImportProcessingListener {

    private static final String GENERIC_FAILURE_REASON =
            "Failed to process the PDF statement. Please try again.";

    private final PdfImportRepository pdfImportRepository;
    private final PdfStorageService pdfStorageService;
    private final PdfInterpretationService pdfInterpretationService;
    private final PdfImportService pdfImportService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("pdfImportTaskExecutor")
    public void onPdfImportUploaded(PdfImportUploadedEvent event) {
        try {
            PdfImport pdfImport = pdfImportRepository.findById(event.importId())
                    .orElseThrow(() -> new PdfImportNotFoundException("PDF import not found"));

            byte[] pdfBytes = pdfStorageService.load(pdfImport.getStoragePath());
            List<ResolvedImportTransaction> resolved =
                    pdfInterpretationService.interpret(event.userId(), pdfBytes);

            pdfImportService.applyInterpretationResult(event.importId(), resolved);
        } catch (Exception e) {
            log.warn("PDF import {} processing failed", event.importId(), e);
            pdfImportService.applyInterpretationFailure(event.importId(), userFacingReason(e));
        }
    }

    private String userFacingReason(Exception e) {
        return e instanceof PdfInterpretationException ? e.getMessage() : GENERIC_FAILURE_REASON;
    }
}
