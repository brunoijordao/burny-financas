package com.burny.financas.agent.controller;

import com.burny.financas.agent.dto.ChatRequest;
import com.burny.financas.agent.dto.ChatResponse;
import com.burny.financas.agent.dto.ConfirmTransactionDraftRequest;
import com.burny.financas.agent.service.AiAgentChatService;
import com.burny.financas.transactions.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Agent", description = "Conversational AI assistant for natural-language financial queries and transaction entry")
@RestController
@RequestMapping("/ai-agent")
@RequiredArgsConstructor
public class AgentController {

    private final AiAgentChatService aiAgentChatService;

    @Operation(summary = "Send a chat message to the AI agent; the frontend resends the open session's "
            + "conversation history with each call, since it is not persisted server-side")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent reply, optionally with a draft transaction to confirm"),
            @ApiResponse(responseCode = "400", description = "Invalid request or conversation history too long"),
            @ApiResponse(responseCode = "429", description = "AI agent chat rate limit exceeded"),
            @ApiResponse(responseCode = "502", description = "The Gemini API call failed, timed out, or returned an unusable response")
    })
    @PostMapping("/messages")
    public ChatResponse sendMessage(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        return aiAgentChatService.chat(currentUserId(authentication), request);
    }

    @Operation(summary = "Confirm a draft transaction proposed by the agent, creating a real transaction "
            + "with the same account-balance effect as a manually entered one. Never calls Gemini.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid draft data"),
            @ApiResponse(responseCode = "404", description = "Account or category not found or not owned by the caller")
    })
    @PostMapping("/transactions/confirm")
    @ResponseStatus(HttpStatus.OK)
    public TransactionResponse confirmDraft(
            @Valid @RequestBody ConfirmTransactionDraftRequest request,
            Authentication authentication
    ) {
        return aiAgentChatService.confirmTransactionDraft(currentUserId(authentication), request);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
