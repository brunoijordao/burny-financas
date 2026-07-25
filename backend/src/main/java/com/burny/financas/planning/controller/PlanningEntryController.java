package com.burny.financas.planning.controller;

import com.burny.financas.planning.dto.CreatePlanningEntryRequest;
import com.burny.financas.planning.dto.PlanningEntryResponse;
import com.burny.financas.planning.dto.ProjectedCashFlowResponse;
import com.burny.financas.planning.dto.UpdatePlanningEntryRequest;
import com.burny.financas.planning.service.PlanningEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@Tag(name = "Planning", description = "Payables and receivables (contas a pagar e a receber) not yet reflected in an account balance, their settlement into real transactions, financial calendar, and projected cash flow")
@RestController
@RequestMapping("/planning-entries")
@RequiredArgsConstructor
public class PlanningEntryController {

    private final PlanningEntryService planningEntryService;

    @Operation(summary = "Create a payable or receivable owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Planning entry created"),
            @ApiResponse(responseCode = "400", description = "Invalid planning entry data"),
            @ApiResponse(responseCode = "404", description = "Account or category not found or not owned by the caller")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanningEntryResponse create(@Valid @RequestBody CreatePlanningEntryRequest request, Authentication authentication) {
        return planningEntryService.create(currentUserId(authentication), request);
    }

    @Operation(summary = "List the authenticated user's active planning entries, ordered by due date")
    @GetMapping
    public List<PlanningEntryResponse> list(Authentication authentication) {
        return planningEntryService.list(currentUserId(authentication));
    }

    @Operation(summary = "Get a single planning entry owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning entry found"),
            @ApiResponse(responseCode = "404", description = "Planning entry not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public PlanningEntryResponse get(@PathVariable Long id, Authentication authentication) {
        return planningEntryService.get(currentUserId(authentication), id);
    }

    @Operation(summary = "Edit a planning entry owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning entry updated"),
            @ApiResponse(responseCode = "404", description = "Planning entry not found or not owned by the caller")
    })
    @PutMapping("/{id}")
    public PlanningEntryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanningEntryRequest request,
            Authentication authentication
    ) {
        return planningEntryService.update(currentUserId(authentication), id, request);
    }

    @Operation(summary = "Delete (soft-delete) a planning entry owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Planning entry deactivated"),
            @ApiResponse(responseCode = "404", description = "Planning entry not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        planningEntryService.delete(currentUserId(authentication), id);
    }

    @Operation(summary = "Settle a planning entry: creates a real transaction via TransactionBalanceService and links it back to the entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning entry settled"),
            @ApiResponse(responseCode = "400", description = "Planning entry is already settled"),
            @ApiResponse(responseCode = "404", description = "Planning entry not found or not owned by the caller")
    })
    @PostMapping("/{id}/settle")
    public PlanningEntryResponse settle(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            Authentication authentication
    ) {
        return planningEntryService.settle(currentUserId(authentication), id, settlementDate);
    }

    @Operation(summary = "Undo a planning entry's settlement, reversing the linked transaction's balance effect and soft-deleting it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement undone"),
            @ApiResponse(responseCode = "400", description = "Planning entry is not settled"),
            @ApiResponse(responseCode = "404", description = "Planning entry not found or not owned by the caller")
    })
    @PostMapping("/{id}/undo-settlement")
    public PlanningEntryResponse undoSettlement(@PathVariable Long id, Authentication authentication) {
        return planningEntryService.undoSettlement(currentUserId(authentication), id);
    }

    @Operation(summary = "Get the authenticated user's active planning entries due within a given calendar month (yyyy-MM, defaults to the current month)")
    @GetMapping("/calendar")
    public List<PlanningEntryResponse> calendar(
            @RequestParam(required = false) String month,
            Authentication authentication
    ) {
        YearMonth resolvedMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        return planningEntryService.getCalendar(currentUserId(authentication), resolvedMonth);
    }

    @Operation(summary = "Get the projected cash flow for the authenticated user across the next N months, from the current available balance plus pending planning entries")
    @GetMapping("/projected-cash-flow")
    public ProjectedCashFlowResponse projectedCashFlow(
            @RequestParam(required = false) Integer months,
            Authentication authentication
    ) {
        return planningEntryService.getProjectedCashFlow(currentUserId(authentication), months);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
