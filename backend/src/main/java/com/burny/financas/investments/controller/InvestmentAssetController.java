package com.burny.financas.investments.controller;

import com.burny.financas.investments.dto.CreateInvestmentAssetRequest;
import com.burny.financas.investments.dto.InvestmentAssetResponse;
import com.burny.financas.investments.dto.UpdateInvestmentAssetRequest;
import com.burny.financas.investments.service.InvestmentAssetService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Investments", description = "Investment assets (stocks, FIIs, CDB, Tesouro Direto, crypto), their computed position/profitability, never affecting any account balance")
@RestController
@RequestMapping("/investments/assets")
@RequiredArgsConstructor
public class InvestmentAssetController {

    private final InvestmentAssetService investmentAssetService;

    @Operation(summary = "Create an investment asset owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Investment asset created"),
            @ApiResponse(responseCode = "400", description = "Invalid asset data or account not owned by the caller")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentAssetResponse create(@Valid @RequestBody CreateInvestmentAssetRequest request, Authentication authentication) {
        return investmentAssetService.create(currentUserId(authentication), request);
    }

    @Operation(summary = "List the authenticated user's active investment assets, with computed position and profitability")
    @GetMapping
    public List<InvestmentAssetResponse> list(Authentication authentication) {
        return investmentAssetService.list(currentUserId(authentication));
    }

    @Operation(summary = "Get a single investment asset owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investment asset found"),
            @ApiResponse(responseCode = "404", description = "Investment asset not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public InvestmentAssetResponse get(@PathVariable Long id, Authentication authentication) {
        return investmentAssetService.get(currentUserId(authentication), id);
    }

    @Operation(summary = "Edit an investment asset owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investment asset updated"),
            @ApiResponse(responseCode = "404", description = "Investment asset not found or not owned by the caller")
    })
    @PutMapping("/{id}")
    public InvestmentAssetResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvestmentAssetRequest request,
            Authentication authentication
    ) {
        return investmentAssetService.update(currentUserId(authentication), id, request);
    }

    @Operation(summary = "Delete (soft-delete) an investment asset owned by the authenticated user, preserving its operations and valuations")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Investment asset deactivated"),
            @ApiResponse(responseCode = "404", description = "Investment asset not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        investmentAssetService.delete(currentUserId(authentication), id);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
