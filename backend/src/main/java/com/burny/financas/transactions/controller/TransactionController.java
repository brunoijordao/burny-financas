package com.burny.financas.transactions.controller;

import com.burny.financas.transactions.dto.CreateTransactionRequest;
import com.burny.financas.transactions.dto.PagedResponse;
import com.burny.financas.transactions.dto.TransactionAttachmentResponse;
import com.burny.financas.transactions.dto.TransactionResponse;
import com.burny.financas.transactions.dto.UpdateTransactionRequest;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.service.TransactionAttachmentService;
import com.burny.financas.transactions.service.TransactionRecurrenceService;
import com.burny.financas.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Transactions", description = "Income and expense transactions, with atomic account balance effects")
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRecurrenceService transactionRecurrenceService;
    private final TransactionAttachmentService transactionAttachmentService;

    @Operation(summary = "Create an income or expense transaction on an account owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
            @ApiResponse(responseCode = "404", description = "Account or category not found or not owned by the caller")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request, Authentication authentication) {
        return transactionService.create(currentUserId(authentication), request);
    }

    @Operation(summary = "List the authenticated user's active transactions, filtered and paginated")
    @GetMapping
    public PagedResponse<TransactionResponse> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return transactionService.list(
                currentUserId(authentication), accountId, categoryId, type, startDate, endDate, page, size);
    }

    @Operation(summary = "Get a single transaction owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id, Authentication authentication) {
        return transactionService.get(currentUserId(authentication), id);
    }

    @Operation(summary = "Edit a transaction, reversing its balance effect and applying the new one atomically")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated"),
            @ApiResponse(responseCode = "404", description = "Transaction, account, or category not found or not owned by the caller")
    })
    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request,
            Authentication authentication
    ) {
        return transactionService.update(currentUserId(authentication), id, request);
    }

    @Operation(summary = "Delete (soft-delete) a transaction, reversing its balance effect")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deactivated"),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        transactionService.delete(currentUserId(authentication), id);
    }

    @Operation(summary = "Cancel a recurrence, stopping future occurrence generation without affecting past occurrences")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recurrence cancelled"),
            @ApiResponse(responseCode = "404", description = "Recurrence not found or not owned by the caller")
    })
    @DeleteMapping("/recurrences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRecurrence(@PathVariable Long id, Authentication authentication) {
        transactionRecurrenceService.cancel(currentUserId(authentication), id);
    }

    @Operation(summary = "Attach a file (image or PDF) to a transaction owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attachment stored"),
            @ApiResponse(responseCode = "400", description = "Unsupported file type"),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by the caller")
    })
    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionAttachmentResponse uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return transactionAttachmentService.upload(currentUserId(authentication), id, file);
    }

    @Operation(summary = "List the attachments of a transaction owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments listed"),
            @ApiResponse(responseCode = "404", description = "Transaction not found or not owned by the caller")
    })
    @GetMapping("/{id}/attachments")
    public List<TransactionAttachmentResponse> listAttachments(@PathVariable Long id, Authentication authentication) {
        return transactionAttachmentService.list(currentUserId(authentication), id);
    }

    @Operation(summary = "Download an attachment of a transaction owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment content"),
            @ApiResponse(responseCode = "404", description = "Transaction or attachment not found or not owned by the caller")
    })
    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        TransactionAttachmentService.LoadedAttachment loaded =
                transactionAttachmentService.download(currentUserId(authentication), id, attachmentId);
        String encodedFilename = java.net.URLEncoder.encode(loaded.originalFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(loaded.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(loaded.resource());
    }

    @Operation(summary = "Remove an attachment from a transaction owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachment removed"),
            @ApiResponse(responseCode = "404", description = "Transaction or attachment not found or not owned by the caller")
    })
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        transactionAttachmentService.delete(currentUserId(authentication), id, attachmentId);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
