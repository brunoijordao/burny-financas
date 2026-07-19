package com.burny.financas.pdfimport.service;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.pdfimport.dto.UpdatePdfImportItemRequest;
import com.burny.financas.pdfimport.entity.PdfImport;
import com.burny.financas.pdfimport.entity.PdfImportItem;
import com.burny.financas.pdfimport.entity.PdfImportItemStatus;
import com.burny.financas.pdfimport.entity.PdfImportStatus;
import com.burny.financas.pdfimport.event.PdfImportUploadedEvent;
import com.burny.financas.pdfimport.exception.InvalidPdfImportDataException;
import com.burny.financas.pdfimport.exception.PdfImportItemNotEditableException;
import com.burny.financas.pdfimport.exception.PdfImportNotFoundException;
import com.burny.financas.pdfimport.repository.PdfImportItemRepository;
import com.burny.financas.pdfimport.repository.PdfImportRepository;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.repository.TransactionRepository;
import com.burny.financas.transactions.service.TransactionBalanceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PdfImportService {

    private final PdfImportRepository pdfImportRepository;
    private final PdfImportItemRepository pdfImportItemRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PdfStorageService pdfStorageService;
    private final TransactionBalanceService transactionBalanceService;
    private final ApplicationEventPublisher eventPublisher;

    public record PdfImportDetail(PdfImport pdfImport, List<PdfImportItem> items) {
    }

    @Transactional
    public PdfImport upload(Long userId, Long accountId, MultipartFile file) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        String storagePath = pdfStorageService.store(userId, file);

        PdfImport pdfImport = PdfImport.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .originalFilename(file.getOriginalFilename())
                .storagePath(storagePath)
                .status(PdfImportStatus.PROCESSING)
                .build();

        PdfImport saved = pdfImportRepository.save(pdfImport);
        eventPublisher.publishEvent(new PdfImportUploadedEvent(saved.getId(), userId));
        return saved;
    }

    @Transactional
    public PdfImport retry(Long userId, Long importId) {
        PdfImport pdfImport = findOwnedOrThrow(importId, userId);
        if (pdfImport.getStatus() != PdfImportStatus.FAILED) {
            throw new InvalidPdfImportDataException("Only a failed import can be retried");
        }

        pdfImport.setStatus(PdfImportStatus.PROCESSING);
        pdfImport.setFailureReason(null);
        PdfImport saved = pdfImportRepository.save(pdfImport);
        eventPublisher.publishEvent(new PdfImportUploadedEvent(saved.getId(), userId));
        return saved;
    }

    @Transactional(readOnly = true)
    public PdfImportDetail get(Long userId, Long importId) {
        PdfImport pdfImport = findOwnedOrThrow(importId, userId);
        List<PdfImportItem> items = pdfImportItemRepository.findAllByPdfImportIdOrderByTransactionDateDesc(importId);
        return new PdfImportDetail(pdfImport, items);
    }

    @Transactional(readOnly = true)
    public List<PdfImport> list(Long userId) {
        return pdfImportRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public PdfImportItem updateItem(Long userId, Long importId, Long itemId, UpdatePdfImportItemRequest request) {
        PdfImportItem item = findOwnedItemOrThrow(userId, importId, itemId);
        requirePending(item);

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        }

        item.setTransactionDate(request.transactionDate());
        item.setDescription(request.description());
        item.setAmount(request.amount());
        item.setType(request.type());
        item.setCategory(category);
        return pdfImportItemRepository.save(item);
    }

    @Transactional
    public void discardItem(Long userId, Long importId, Long itemId) {
        PdfImportItem item = findOwnedItemOrThrow(userId, importId, itemId);
        requirePending(item);
        item.setStatus(PdfImportItemStatus.DISCARDED);
        pdfImportItemRepository.save(item);
    }

    @Transactional
    public PdfImportItem confirmItem(Long userId, Long importId, Long itemId) {
        PdfImportItem item = findOwnedItemOrThrow(userId, importId, itemId);
        requirePending(item);

        Account account = accountRepository.findByIdForUpdate(item.getPdfImport().getAccount().getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        transactionBalanceService.applyEffect(account, item.getType(), item.getAmount());
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .category(item.getCategory())
                .type(item.getType())
                .amount(item.getAmount())
                .description(item.getDescription())
                .transactionDate(item.getTransactionDate())
                .active(true)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        item.setStatus(PdfImportItemStatus.CONFIRMED);
        item.setTransaction(savedTransaction);
        return pdfImportItemRepository.save(item);
    }

    /**
     * Called by {@code PdfImportProcessingListener} after successful extraction+interpretation.
     * Cross-bean call (listener -&gt; service), never self-invoked, so this method's own
     * {@code @Transactional} proxy always applies.
     */
    @Transactional
    public void applyInterpretationResult(Long importId, List<ResolvedImportTransaction> resolved) {
        PdfImport pdfImport = pdfImportRepository.findById(importId)
                .orElseThrow(() -> new PdfImportNotFoundException("PDF import not found"));

        for (ResolvedImportTransaction resolvedTransaction : resolved) {
            PdfImportItem item = PdfImportItem.builder()
                    .pdfImport(pdfImport)
                    .transactionDate(resolvedTransaction.transactionDate())
                    .description(resolvedTransaction.description())
                    .amount(resolvedTransaction.amount())
                    .type(resolvedTransaction.type())
                    .category(resolvedTransaction.category())
                    .status(PdfImportItemStatus.PENDING)
                    .build();
            pdfImportItemRepository.save(item);
        }

        pdfImport.setStatus(PdfImportStatus.READY_FOR_REVIEW);
        pdfImport.setFailureReason(null);
        pdfImportRepository.save(pdfImport);
    }

    /** Called by {@code PdfImportProcessingListener} when extraction or interpretation fails. */
    @Transactional
    public void applyInterpretationFailure(Long importId, String reason) {
        PdfImport pdfImport = pdfImportRepository.findById(importId)
                .orElseThrow(() -> new PdfImportNotFoundException("PDF import not found"));
        pdfImport.setStatus(PdfImportStatus.FAILED);
        pdfImport.setFailureReason(reason);
        pdfImportRepository.save(pdfImport);
    }

    private PdfImport findOwnedOrThrow(Long importId, Long userId) {
        return pdfImportRepository.findByIdAndUserId(importId, userId)
                .orElseThrow(() -> new PdfImportNotFoundException("PDF import not found"));
    }

    private PdfImportItem findOwnedItemOrThrow(Long userId, Long importId, Long itemId) {
        findOwnedOrThrow(importId, userId);
        return pdfImportItemRepository.findByIdAndPdfImportId(itemId, importId)
                .orElseThrow(() -> new PdfImportNotFoundException("PDF import item not found"));
    }

    private void requirePending(PdfImportItem item) {
        if (item.getStatus() != PdfImportItemStatus.PENDING) {
            throw new PdfImportItemNotEditableException("Line item is not pending review");
        }
    }
}
