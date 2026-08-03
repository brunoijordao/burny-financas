package com.burny.financas.agent.service;

import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.agent.config.AiAgentProperties;
import com.burny.financas.agent.dto.ChatMessage;
import com.burny.financas.agent.dto.ChatRequest;
import com.burny.financas.agent.dto.ChatResponse;
import com.burny.financas.agent.dto.ChatRole;
import com.burny.financas.agent.dto.ConfirmTransactionDraftRequest;
import com.burny.financas.agent.dto.TransactionDraft;
import com.burny.financas.agent.exception.InvalidChatRequestException;
import com.burny.financas.agent.service.tool.AgentToolCatalog;
import com.burny.financas.agent.service.tool.AgentToolDispatcher;
import com.burny.financas.agent.service.tool.ToolDispatchResult;
import com.burny.financas.categories.service.CategoryService;
import com.burny.financas.transactions.dto.CreateTransactionRequest;
import com.burny.financas.transactions.dto.TransactionResponse;
import com.burny.financas.transactions.service.TransactionService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one chat turn (design.md Decision 2): builds the per-request system instruction and
 * tool declarations, calls Gemini, dispatches at most one tool call per model turn, and calls
 * Gemini again with the tool's result. Bounded at {@link #MAX_MODEL_CALLS} Gemini calls total so a
 * pathological tool-call loop can't hang the request thread indefinitely.
 *
 * <p>{@link #confirmTransactionDraft} is a separate, deterministic path that never calls Gemini
 * (design.md Decision 4): the model can only ever produce a draft, never trigger a write itself.
 */
@Service
@RequiredArgsConstructor
public class AiAgentChatService {

    private static final int MAX_MODEL_CALLS = 3;
    private static final String FALLBACK_MESSAGE =
            "Não consegui concluir sua solicitação agora. Poderia reformular ou tentar novamente em instantes?";

    private final AiAgentClient aiAgentClient;
    private final AgentContextBuilder contextBuilder;
    private final AgentToolDispatcher toolDispatcher;
    private final AiAgentProperties aiAgentProperties;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    public ChatResponse chat(Long userId, ChatRequest request) {
        List<ChatMessage> history = request.history();
        if (history.size() > aiAgentProperties.getMaxHistoryMessages()) {
            throw new InvalidChatRequestException(
                    "Conversation history exceeds the maximum of " + aiAgentProperties.getMaxHistoryMessages() + " messages");
        }

        AgentContext context = contextBuilder.build(userId);

        List<GeminiApiTypes.Content> contents = new ArrayList<>();
        for (ChatMessage message : history) {
            contents.add(new GeminiApiTypes.Content(toGeminiRole(message.role()), List.of(GeminiApiTypes.Part.ofText(message.text()))));
        }
        contents.add(new GeminiApiTypes.Content("user", List.of(GeminiApiTypes.Part.ofText(request.message()))));

        List<GeminiApiTypes.Tool> tools =
                List.of(new GeminiApiTypes.Tool(AgentToolCatalog.declarations(context.accounts(), context.categories())));

        TransactionDraft draft = null;
        for (int call = 0; call < MAX_MODEL_CALLS; call++) {
            ModelTurn turn = aiAgentClient.generate(contents, context.systemInstructionText(), tools);

            if (turn instanceof ModelTurn.TextTurn textTurn) {
                return new ChatResponse(textTurn.text(), draft);
            }

            GeminiApiTypes.FunctionCall functionCall = ((ModelTurn.FunctionCallTurn) turn).functionCall();
            ToolDispatchResult dispatchResult = toolDispatcher.dispatch(functionCall.name(), functionCall.args(), userId);
            if (dispatchResult.draft() != null) {
                draft = dispatchResult.draft();
            }

            contents.add(new GeminiApiTypes.Content("model", List.of(GeminiApiTypes.Part.ofFunctionCall(functionCall))));
            contents.add(new GeminiApiTypes.Content("user", List.of(GeminiApiTypes.Part.ofFunctionResponse(
                    new GeminiApiTypes.FunctionResponse(functionCall.name(), dispatchResult.functionResponsePayload())))));
        }

        return new ChatResponse(FALLBACK_MESSAGE, draft);
    }

    /**
     * Never calls Gemini. Re-validates {@code accountId}/{@code categoryId} ownership (design.md
     * Decision 5) before delegating to {@code TransactionService.create}, identical in effect to a
     * manually entered transaction.
     */
    public TransactionResponse confirmTransactionDraft(Long userId, ConfirmTransactionDraftRequest request) {
        accountService.get(userId, request.accountId());
        if (request.categoryId() != null) {
            categoryService.get(userId, request.categoryId());
        }

        LocalDate transactionDate = request.date() != null ? request.date() : LocalDate.now();
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                request.description(),
                request.amount(),
                request.type(),
                transactionDate,
                request.accountId(),
                request.categoryId(),
                null,
                false,
                null,
                null,
                null);

        return transactionService.create(userId, createRequest);
    }

    private String toGeminiRole(ChatRole role) {
        return role == ChatRole.MODEL ? "model" : "user";
    }
}
