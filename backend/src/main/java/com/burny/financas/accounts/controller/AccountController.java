package com.burny.financas.accounts.controller;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.dto.ConsolidatedBalanceResponse;
import com.burny.financas.accounts.dto.CreateAccountRequest;
import com.burny.financas.accounts.dto.TransferRequest;
import com.burny.financas.accounts.dto.TransferResponse;
import com.burny.financas.accounts.dto.UpdateAccountRequest;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.accounts.service.AccountTransferService;
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

@Tag(name = "Accounts", description = "Account management, balances, and transfers")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountTransferService accountTransferService;

    @Operation(summary = "Create a new account owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid account data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request, Authentication authentication) {
        return accountService.create(currentUserId(authentication), request);
    }

    @Operation(summary = "List the authenticated user's active accounts")
    @GetMapping
    public List<AccountResponse> list(Authentication authentication) {
        return accountService.list(currentUserId(authentication));
    }

    @Operation(summary = "Get the consolidated balance across all active accounts")
    @GetMapping("/balance")
    public ConsolidatedBalanceResponse consolidatedBalance(Authentication authentication) {
        return accountService.getConsolidatedBalance(currentUserId(authentication));
    }

    @Operation(summary = "Get a single account owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id, Authentication authentication) {
        return accountService.get(currentUserId(authentication), id);
    }

    @Operation(summary = "Edit an account's name, icon, color, or credit limit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated"),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the caller")
    })
    @PutMapping("/{id}")
    public AccountResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication
    ) {
        return accountService.update(currentUserId(authentication), id, request);
    }

    @Operation(summary = "Delete an account (soft-deleted instead if it has linked history)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted or deactivated"),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        accountService.delete(currentUserId(authentication), id);
    }

    @Operation(summary = "Transfer an amount between two of the authenticated user's own accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer completed"),
            @ApiResponse(responseCode = "400", description = "Non-positive amount"),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found or not owned by the caller"),
            @ApiResponse(responseCode = "422", description = "Transfer not allowed (credit card as source, insufficient balance, inactive account)")
    })
    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request, Authentication authentication) {
        return accountTransferService.transfer(currentUserId(authentication), request);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
