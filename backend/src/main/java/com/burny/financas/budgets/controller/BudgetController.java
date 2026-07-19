package com.burny.financas.budgets.controller;

import com.burny.financas.budgets.dto.BudgetResponse;
import com.burny.financas.budgets.dto.SetBudgetRequest;
import com.burny.financas.budgets.service.BudgetService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Budgets", description = "Monthly per-category spending limits, computed against the caller's own transactions")
@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "Set (create or update) the authenticated user's current-month budget for a category they own")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget set"),
            @ApiResponse(responseCode = "404", description = "Category not found or not owned by the caller")
    })
    @PutMapping("/categories/{categoryId}")
    public BudgetResponse setBudget(
            @PathVariable Long categoryId,
            @Valid @RequestBody SetBudgetRequest request,
            Authentication authentication
    ) {
        return budgetService.setBudget(currentUserId(authentication), categoryId, request.limitAmount());
    }

    @Operation(summary = "List the authenticated user's budgets for the current month, with computed spend")
    @GetMapping
    public List<BudgetResponse> list(Authentication authentication) {
        return budgetService.list(currentUserId(authentication));
    }

    @Operation(summary = "Delete (soft-delete) a budget owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Budget deactivated"),
            @ApiResponse(responseCode = "404", description = "Budget not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        budgetService.delete(currentUserId(authentication), id);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
