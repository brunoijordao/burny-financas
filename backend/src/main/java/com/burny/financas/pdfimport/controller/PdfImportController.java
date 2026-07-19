package com.burny.financas.pdfimport.controller;

import com.burny.financas.pdfimport.dto.PdfImportDetailResponse;
import com.burny.financas.pdfimport.dto.PdfImportItemResponse;
import com.burny.financas.pdfimport.dto.PdfImportResponse;
import com.burny.financas.pdfimport.dto.UpdatePdfImportItemRequest;
import com.burny.financas.pdfimport.mapper.PdfImportMapper;
import com.burny.financas.pdfimport.service.PdfImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@Tag(name = "PDF Imports", description = "Upload and review AI-interpreted Itau PDF statements before creating transactions")
@RestController
@RequestMapping("/pdf-imports")
@RequiredArgsConstructor
public class PdfImportController {

    private final PdfImportService pdfImportService;
    private final PdfImportMapper pdfImportMapper;

    @Operation(summary = "Upload a PDF statement for an account owned by the authenticated user; "
            + "processing (extraction + AI interpretation) happens asynchronously")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Upload accepted, processing started"),
            @ApiResponse(responseCode = "400", description = "Missing account, or file is not a PDF"),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the caller"),
            @ApiResponse(responseCode = "429", description = "PDF upload rate limit exceeded")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PdfImportResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("accountId") Long accountId,
            Authentication authentication
    ) {
        return pdfImportMapper.toResponse(pdfImportService.upload(currentUserId(authentication), accountId, file));
    }

    @Operation(summary = "List the authenticated user's PDF imports")
    @GetMapping
    public List<PdfImportResponse> list(Authentication authentication) {
        return pdfImportService.list(currentUserId(authentication)).stream()
                .map(pdfImportMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Get a PDF import owned by the authenticated user, including its line items")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import found"),
            @ApiResponse(responseCode = "404", description = "Import not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public PdfImportDetailResponse get(@PathVariable Long id, Authentication authentication) {
        return pdfImportMapper.toDetailResponse(pdfImportService.get(currentUserId(authentication), id));
    }

    @Operation(summary = "Retry a failed import's extraction/interpretation without re-uploading the file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retry started"),
            @ApiResponse(responseCode = "400", description = "Import is not in a failed state"),
            @ApiResponse(responseCode = "404", description = "Import not found or not owned by the caller")
    })
    @PostMapping("/{id}/retry")
    public PdfImportResponse retry(@PathVariable Long id, Authentication authentication) {
        return pdfImportMapper.toResponse(pdfImportService.retry(currentUserId(authentication), id));
    }

    @Operation(summary = "Edit a pending line item before confirming it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line item updated"),
            @ApiResponse(responseCode = "404", description = "Import, item, or category not found or not owned by the caller"),
            @ApiResponse(responseCode = "409", description = "Line item is not pending")
    })
    @PutMapping("/{id}/items/{itemId}")
    public PdfImportItemResponse updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdatePdfImportItemRequest request,
            Authentication authentication
    ) {
        return pdfImportMapper.toResponse(
                pdfImportService.updateItem(currentUserId(authentication), id, itemId, request));
    }

    @Operation(summary = "Discard a pending line item so it is never confirmed into a transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Line item discarded"),
            @ApiResponse(responseCode = "404", description = "Import or item not found or not owned by the caller"),
            @ApiResponse(responseCode = "409", description = "Line item is not pending")
    })
    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardItem(@PathVariable Long id, @PathVariable Long itemId, Authentication authentication) {
        pdfImportService.discardItem(currentUserId(authentication), id, itemId);
    }

    @Operation(summary = "Confirm a pending line item, creating a real transaction with the same "
            + "account-balance effect as a manually created one")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line item confirmed and transaction created"),
            @ApiResponse(responseCode = "404", description = "Import or item not found or not owned by the caller"),
            @ApiResponse(responseCode = "409", description = "Line item is not pending")
    })
    @PostMapping("/{id}/items/{itemId}/confirm")
    public PdfImportItemResponse confirmItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        return pdfImportMapper.toResponse(pdfImportService.confirmItem(currentUserId(authentication), id, itemId));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
