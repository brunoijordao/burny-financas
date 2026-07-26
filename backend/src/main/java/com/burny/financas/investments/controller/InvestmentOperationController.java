package com.burny.financas.investments.controller;

import com.burny.financas.investments.dto.CreateInvestmentOperationRequest;
import com.burny.financas.investments.dto.InvestmentOperationResponse;
import com.burny.financas.investments.service.InvestmentOperationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Investment Operations", description = "Buy (aporte) and sell (resgate) operations against an investment asset, never affecting any account balance")
@RestController
@RequestMapping("/investments/assets/{assetId}/operations")
@RequiredArgsConstructor
public class InvestmentOperationController {

    private final InvestmentOperationService investmentOperationService;

    @Operation(summary = "Record a buy or sell operation against an asset owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operation recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid operation data, or a sell exceeding the asset's current quantity"),
            @ApiResponse(responseCode = "404", description = "Asset not found or not owned by the caller")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentOperationResponse create(
            @PathVariable Long assetId,
            @Valid @RequestBody CreateInvestmentOperationRequest request,
            Authentication authentication
    ) {
        return investmentOperationService.create(currentUserId(authentication), assetId, request);
    }

    @Operation(summary = "List the active operations recorded against an asset owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operations listed"),
            @ApiResponse(responseCode = "404", description = "Asset not found or not owned by the caller")
    })
    @GetMapping
    public List<InvestmentOperationResponse> list(@PathVariable Long assetId, Authentication authentication) {
        return investmentOperationService.list(currentUserId(authentication), assetId);
    }

    @Operation(summary = "Delete (soft-delete) an operation, excluding it from the asset's recomputed position")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operation deactivated"),
            @ApiResponse(responseCode = "404", description = "Asset or operation not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long assetId, @PathVariable Long id, Authentication authentication) {
        investmentOperationService.delete(currentUserId(authentication), assetId, id);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
