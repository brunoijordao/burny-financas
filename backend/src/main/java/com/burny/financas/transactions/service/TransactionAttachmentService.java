package com.burny.financas.transactions.service;

import com.burny.financas.transactions.dto.TransactionAttachmentResponse;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionAttachment;
import com.burny.financas.transactions.exception.TransactionNotFoundException;
import com.burny.financas.transactions.repository.TransactionAttachmentRepository;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TransactionAttachmentService {

    private final TransactionRepository transactionRepository;
    private final TransactionAttachmentRepository transactionAttachmentRepository;
    private final FileStorageService fileStorageService;

    public record LoadedAttachment(Resource resource, String originalFilename, String contentType) {
    }

    @Transactional
    public TransactionAttachmentResponse upload(Long userId, Long transactionId, MultipartFile file) {
        Transaction transaction = findOwnedTransactionOrThrow(transactionId, userId);

        FileStorageService.StoredFile stored = fileStorageService.store(userId, file);

        TransactionAttachment attachment = TransactionAttachment.builder()
                .transaction(transaction)
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .storagePath(stored.storagePath())
                .build();

        return toResponse(transactionAttachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<TransactionAttachmentResponse> list(Long userId, Long transactionId) {
        findOwnedTransactionOrThrow(transactionId, userId);
        return transactionAttachmentRepository.findAllByTransactionId(transactionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoadedAttachment download(Long userId, Long transactionId, Long attachmentId) {
        findOwnedTransactionOrThrow(transactionId, userId);
        TransactionAttachment attachment = findOwnedAttachmentOrThrow(transactionId, attachmentId);
        Resource resource = fileStorageService.load(attachment.getStoragePath());
        return new LoadedAttachment(resource, attachment.getOriginalFilename(), attachment.getContentType());
    }

    @Transactional
    public void delete(Long userId, Long transactionId, Long attachmentId) {
        findOwnedTransactionOrThrow(transactionId, userId);
        TransactionAttachment attachment = findOwnedAttachmentOrThrow(transactionId, attachmentId);
        fileStorageService.delete(attachment.getStoragePath());
        transactionAttachmentRepository.delete(attachment);
    }

    private Transaction findOwnedTransactionOrThrow(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }

    private TransactionAttachment findOwnedAttachmentOrThrow(Long transactionId, Long attachmentId) {
        return transactionAttachmentRepository.findByIdAndTransactionId(attachmentId, transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Attachment not found"));
    }

    private TransactionAttachmentResponse toResponse(TransactionAttachment attachment) {
        return new TransactionAttachmentResponse(
                attachment.getId(),
                attachment.getTransaction().getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}
