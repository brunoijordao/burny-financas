package com.burny.financas.investments.controller;

import com.burny.financas.investments.dto.CreateInvestmentValuationRequest;
import com.burny.financas.investments.dto.InvestmentValuationResponse;
import com.burny.financas.investments.service.InvestmentValuationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Investment Valuations", description = "Manually entered, dated current-value snapshots for an investment asset — no real-time quote integration in this change")
@RestController
@RequestMapping("/investments/assets/{assetId}/valuations")
@RequiredArgsConstructor
public class InvestmentValuationController {

    private final InvestmentValuationService investmentValuationService;

    @Operation(summary = "Record a manual current-value snapshot for an asset owned by the authenticated user; prior valuations are kept as history")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Valuation recorded"),
            @ApiResponse(responseCode = "404", description = "Asset not found or not owned by the caller")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentValuationResponse create(
            @PathVariable Long assetId,
            @Valid @RequestBody CreateInvestmentValuationRequest request,
            Authentication authentication
    ) {
        return investmentValuationService.create(currentUserId(authentication), assetId, request);
    }

    @Operation(summary = "List the valuation history recorded for an asset owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valuations listed"),
            @ApiResponse(responseCode = "404", description = "Asset not found or not owned by the caller")
    })
    @GetMapping
    public List<InvestmentValuationResponse> list(@PathVariable Long assetId, Authentication authentication) {
        return investmentValuationService.list(currentUserId(authentication), assetId);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
