package com.burny.financas.pdfimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.entity.AccountType;
import com.burny.financas.accounts.exception.AccountNotFoundException;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.pdfimport.entity.PdfImport;
import com.burny.financas.pdfimport.entity.PdfImportItem;
import com.burny.financas.pdfimport.entity.PdfImportItemStatus;
import com.burny.financas.pdfimport.entity.PdfImportStatus;
import com.burny.financas.pdfimport.event.PdfImportUploadedEvent;
import com.burny.financas.pdfimport.exception.InvalidPdfImportDataException;
import com.burny.financas.pdfimport.exception.PdfImportItemNotEditableException;
import com.burny.financas.pdfimport.repository.PdfImportItemRepository;
import com.burny.financas.pdfimport.repository.PdfImportRepository;
import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import com.burny.financas.transactions.service.TransactionBalanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PdfImportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PdfImportRepository pdfImportRepository;
    @Mock
    private PdfImportItemRepository pdfImportItemRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PdfStorageService pdfStorageService;
    @Mock
    private TransactionBalanceService transactionBalanceService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PdfImportService service() {
        return new PdfImportService(
                pdfImportRepository, pdfImportItemRepository, accountRepository, categoryRepository,
                userRepository, transactionRepository, pdfStorageService, transactionBalanceService, eventPublisher);
    }

    private Account ownedAccount(Long accountId) {
        return Account.builder().id(accountId).type(AccountType.CHECKING).balance(new BigDecimal("100.00")).build();
    }

    private PdfImport importWith(PdfImportStatus status, Account account) {
        return PdfImport.builder().id(5L).status(status).account(account).storagePath("/tmp/x.pdf").build();
    }

    private PdfImportItem pendingItem(PdfImport pdfImport, TransactionType type, String amount) {
        return PdfImportItem.builder()
                .id(20L)
                .pdfImport(pdfImport)
                .transactionDate(LocalDate.of(2026, 1, 15))
                .description("COMPRA")
                .amount(new BigDecimal(amount))
                .type(type)
                .status(PdfImportItemStatus.PENDING)
                .build();
    }

    @Test
    void uploadRejectsAccountNotOwnedByCaller() {
        when(accountRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> service().upload(USER_ID, 2L, file))
                .isInstanceOf(AccountNotFoundException.class);

        verify(pdfImportRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void uploadStoresFilePersistsProcessingImportAndPublishesEvent() {
        Account account = ownedAccount(2L);
        when(accountRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(account));
        when(pdfStorageService.store(eq(USER_ID), any())).thenReturn("/data/pdf-imports/1/uuid.pdf");
        when(pdfImportRepository.save(any(PdfImport.class))).thenAnswer(inv -> {
            PdfImport pdfImport = inv.getArgument(0);
            pdfImport.setId(5L);
            return pdfImport;
        });
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", "x".getBytes());

        PdfImport result = service().upload(USER_ID, 2L, file);

        assertThat(result.getStatus()).isEqualTo(PdfImportStatus.PROCESSING);
        assertThat(result.getStoragePath()).isEqualTo("/data/pdf-imports/1/uuid.pdf");

        ArgumentCaptor<PdfImportUploadedEvent> eventCaptor = ArgumentCaptor.forClass(PdfImportUploadedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().importId()).isEqualTo(5L);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
    }

    @Test
    void retryRejectsImportThatIsNotFailed() {
        PdfImport pdfImport = importWith(PdfImportStatus.READY_FOR_REVIEW, ownedAccount(2L));
        when(pdfImportRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(pdfImport));

        assertThatThrownBy(() -> service().retry(USER_ID, 5L))
                .isInstanceOf(InvalidPdfImportDataException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void retryResetsFailedImportToProcessingAndPublishesEvent() {
        PdfImport pdfImport = importWith(PdfImportStatus.FAILED, ownedAccount(2L));
        pdfImport.setFailureReason("Gemma unavailable");
        when(pdfImportRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(pdfImport));
        when(pdfImportRepository.save(any(PdfImport.class))).thenAnswer(inv -> inv.getArgument(0));

        PdfImport result = service().retry(USER_ID, 5L);

        assertThat(result.getStatus()).isEqualTo(PdfImportStatus.PROCESSING);
        assertThat(result.getFailureReason()).isNull();
        verify(eventPublisher).publishEvent(any(PdfImportUploadedEvent.class));
    }

    @Test
    void discardRejectsItemThatIsNotPending() {
        PdfImport pdfImport = importWith(PdfImportStatus.READY_FOR_REVIEW, ownedAccount(2L));
        PdfImportItem item = pendingItem(pdfImport, TransactionType.EXPENSE, "10.00");
        item.setStatus(PdfImportItemStatus.CONFIRMED);
        when(pdfImportRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(pdfImport));
        when(pdfImportItemRepository.findByIdAndPdfImportId(20L, 5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service().discardItem(USER_ID, 5L, 20L))
                .isInstanceOf(PdfImportItemNotEditableException.class);

        verify(pdfImportItemRepository, never()).save(any());
    }

    @Test
    void discardMarksPendingItemAsDiscarded() {
        PdfImport pdfImport = importWith(PdfImportStatus.READY_FOR_REVIEW, ownedAccount(2L));
        PdfImportItem item = pendingItem(pdfImport, TransactionType.EXPENSE, "10.00");
        when(pdfImportRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(pdfImport));
        when(pdfImportItemRepository.findByIdAndPdfImportId(20L, 5L)).thenReturn(Optional.of(item));
        when(pdfImportItemRepository.save(any(PdfImportItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service().discardItem(USER_ID, 5L, 20L);

        assertThat(item.getStatus()).isEqualTo(PdfImportItemStatus.DISCARDED);
    }

    @Test
    void confirmRejectsItemThatIsNotPending() {
        PdfImport pdfImport = importWith(PdfImportStatus.READY_FOR_REVIEW, ownedAccount(2L));
        PdfImportItem item = pendingItem(pdfImport, TransactionType.EXPENSE, "10.00");
        item.setStatus(PdfImportItemStatus.DISCARDED);
        when(pdfImportRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(pdfImport));
        when(pdfImportItemRepository.findByIdAndPdfImportId(20L, 5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service().confirmItem(USER_ID, 5L, 20L))
                .isInstanceOf(PdfImportItemNotEditableException.class);

        verify(transactionBalanceService, never()).applyEffect(any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void confirmAppliesBalanceEffectCreatesTransactionAndMarksItemConfirmed() {
        Account account = ownedAccount(2L);
        PdfImport pdfImport = importWith(PdfImportStatus.READY_FOR_REVIEW, account);
        PdfImportItem item = pendingItem(pdfImport, TransactionType.EXPENSE, "30.00");

        when(pdfImportRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(pdfImport));
        when(pdfImportItemRepository.findByIdAndPdfImportId(20L, 5L)).thenReturn(Optional.of(item));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction transaction = inv.getArgument(0);
            transaction.setId(99L);
            return transaction;
        });
        when(pdfImportItemRepository.save(any(PdfImportItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PdfImportItem result = service().confirmItem(USER_ID, 5L, 20L);

        verify(transactionBalanceService).applyEffect(account, TransactionType.EXPENSE, new BigDecimal("30.00"));
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo("30.00");
        assertThat(transactionCaptor.getValue().getAccount()).isEqualTo(account);

        assertThat(result.getStatus()).isEqualTo(PdfImportItemStatus.CONFIRMED);
        assertThat(result.getTransaction().getId()).isEqualTo(99L);
    }
}
