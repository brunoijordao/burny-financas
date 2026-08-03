package com.burny.financas.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.dto.AccountResponse;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.agent.config.AiAgentProperties;
import com.burny.financas.agent.dto.ChatRequest;
import com.burny.financas.agent.dto.ChatResponse;
import com.burny.financas.agent.dto.ConfirmTransactionDraftRequest;
import com.burny.financas.agent.dto.TransactionDraft;
import com.burny.financas.agent.exception.InvalidChatRequestException;
import com.burny.financas.agent.service.tool.AgentToolDispatcher;
import com.burny.financas.agent.service.tool.ToolDispatchResult;
import com.burny.financas.categories.service.CategoryService;
import com.burny.financas.transactions.dto.CreateTransactionRequest;
import com.burny.financas.transactions.dto.TransactionResponse;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAgentChatServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private AiAgentClient aiAgentClient;
    @Mock
    private AgentContextBuilder contextBuilder;
    @Mock
    private AgentToolDispatcher toolDispatcher;
    @Mock
    private AiAgentProperties aiAgentProperties;
    @Mock
    private AccountService accountService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionService transactionService;

    private AiAgentChatService service() {
        return new AiAgentChatService(
                aiAgentClient, contextBuilder, toolDispatcher, aiAgentProperties, accountService, categoryService, transactionService);
    }

    private void stubContext() {
        when(contextBuilder.build(USER_ID)).thenReturn(new AgentContext("system instruction", List.of(), List.of()));
    }

    private AccountResponse account(Long id, String name) {
        return new AccountResponse(id, name, "wallet", "#000", AccountType.CHECKING, true,
                new BigDecimal("100.00"), null, null, null, null);
    }

    @Test
    void textOnlyTurnReturnsReplyWithoutCallingAnyTool() {
        stubContext();
        when(aiAgentClient.generate(any(), any(), any())).thenReturn(new ModelTurn.TextTurn("Seu saldo e R$100"));

        ChatResponse response = service().chat(USER_ID, new ChatRequest("qual meu saldo?", List.of()));

        assertThat(response.reply()).isEqualTo("Seu saldo e R$100");
        assertThat(response.draft()).isNull();
        verify(aiAgentClient, times(1)).generate(any(), any(), any());
        verify(toolDispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void oneToolCallTurnDispatchesThenReturnsTheSecondCallsText() {
        stubContext();
        GeminiApiTypes.FunctionCall functionCall = new GeminiApiTypes.FunctionCall("getBudgetStatus", Map.of());
        when(aiAgentClient.generate(any(), any(), any()))
                .thenReturn(new ModelTurn.FunctionCallTurn(functionCall))
                .thenReturn(new ModelTurn.TextTurn("Voce nao tem orcamentos este mes."));
        when(toolDispatcher.dispatch(eq("getBudgetStatus"), eq(Map.of()), eq(USER_ID)))
                .thenReturn(ToolDispatchResult.of(Map.of("budgets", List.of())));

        ChatResponse response = service().chat(USER_ID, new ChatRequest("como estao meus orcamentos?", List.of()));

        assertThat(response.reply()).isEqualTo("Voce nao tem orcamentos este mes.");
        assertThat(response.draft()).isNull();
        verify(aiAgentClient, times(2)).generate(any(), any(), any());
        verify(toolDispatcher, times(1)).dispatch(any(), any(), any());
    }

    @Test
    void proposeTransactionTurnReturnsDraftAlongsideTheFinalReply() {
        stubContext();
        TransactionDraft draft = new TransactionDraft(
                7L, "Conta Corrente", TransactionType.EXPENSE, new BigDecimal("50"), "Fast food", null, null, LocalDate.now());
        GeminiApiTypes.FunctionCall functionCall = new GeminiApiTypes.FunctionCall("proposeTransaction", Map.of());
        when(aiAgentClient.generate(any(), any(), any()))
                .thenReturn(new ModelTurn.FunctionCallTurn(functionCall))
                .thenReturn(new ModelTurn.TextTurn("Confirma o lancamento?"));
        when(toolDispatcher.dispatch(eq("proposeTransaction"), any(), eq(USER_ID)))
                .thenReturn(ToolDispatchResult.withDraft(Map.of("status", "draft_ready"), draft));

        ChatResponse response = service().chat(USER_ID, new ChatRequest("gastei 50 com fast food", List.of()));

        assertThat(response.reply()).isEqualTo("Confirma o lancamento?");
        assertThat(response.draft()).isEqualTo(draft);
        verify(transactionService, never()).create(any(), any());
    }

    @Test
    void repeatedToolCallsHitTheLoopBoundAndReturnAFallbackMessage() {
        stubContext();
        GeminiApiTypes.FunctionCall functionCall = new GeminiApiTypes.FunctionCall("getBudgetStatus", Map.of());
        when(aiAgentClient.generate(any(), any(), any())).thenReturn(new ModelTurn.FunctionCallTurn(functionCall));
        when(toolDispatcher.dispatch(any(), any(), any())).thenReturn(ToolDispatchResult.of(Map.of("budgets", List.of())));

        ChatResponse response = service().chat(USER_ID, new ChatRequest("como estao meus orcamentos?", List.of()));

        assertThat(response.reply()).contains("Não consegui concluir");
        verify(aiAgentClient, times(3)).generate(any(), any(), any());
        verify(toolDispatcher, times(3)).dispatch(any(), any(), any());
    }

    @Test
    void historyExceedingTheConfiguredMaximumIsRejectedWithoutCallingGemini() {
        when(aiAgentProperties.getMaxHistoryMessages()).thenReturn(1);

        assertThatThrownBy(() -> service().chat(USER_ID, new ChatRequest("oi", List.of(
                new com.burny.financas.agent.dto.ChatMessage(com.burny.financas.agent.dto.ChatRole.USER, "a"),
                new com.burny.financas.agent.dto.ChatMessage(com.burny.financas.agent.dto.ChatRole.MODEL, "b")))))
                .isInstanceOf(InvalidChatRequestException.class);

        verify(aiAgentClient, never()).generate(any(), any(), any());
    }

    @Test
    void confirmingAValidDraftCreatesATransactionWithTheDraftsFields() {
        when(accountService.get(USER_ID, 7L)).thenReturn(account(7L, "Conta Corrente"));
        TransactionResponse created = new TransactionResponse(
                99L, "Fast food", new BigDecimal("50"), TransactionType.EXPENSE, LocalDate.now(), 7L, null, null, null, true, null, null);
        when(transactionService.create(eq(USER_ID), any())).thenReturn(created);

        TransactionResponse result = service().confirmTransactionDraft(USER_ID, new ConfirmTransactionDraftRequest(
                "Fast food", new BigDecimal("50"), TransactionType.EXPENSE, LocalDate.now(), 7L, null));

        assertThat(result).isEqualTo(created);
        ArgumentCaptor<CreateTransactionRequest> captor = ArgumentCaptor.forClass(CreateTransactionRequest.class);
        verify(transactionService).create(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().accountId()).isEqualTo(7L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50");
        assertThat(captor.getValue().description()).isEqualTo("Fast food");
    }

    @Test
    void confirmingADraftWithAnAccountNotOwnedByTheCallerNeverCreatesATransaction() {
        when(accountService.get(USER_ID, 999L)).thenThrow(new AccountNotFoundException("Account not found"));

        assertThatThrownBy(() -> service().confirmTransactionDraft(USER_ID, new ConfirmTransactionDraftRequest(
                "Fast food", new BigDecimal("50"), TransactionType.EXPENSE, LocalDate.now(), 999L, null)))
                .isInstanceOf(AccountNotFoundException.class);

        verify(transactionService, never()).create(any(), any());
    }
}
