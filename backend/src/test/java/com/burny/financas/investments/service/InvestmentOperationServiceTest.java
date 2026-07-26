package com.burny.financas.investments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.investments.dto.CreateInvestmentOperationRequest;
import com.burny.financas.investments.dto.InvestmentOperationResponse;
import com.burny.financas.investments.entity.AssetType;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentOperation;
import com.burny.financas.investments.entity.OperationType;
import com.burny.financas.investments.exception.InvalidInvestmentDataException;
import com.burny.financas.investments.mapper.InvestmentAssetMapperImpl;
import com.burny.financas.investments.repository.InvestmentAssetRepository;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestmentOperationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ASSET_ID = 10L;

    @Mock
    private InvestmentOperationRepository investmentOperationRepository;
    @Mock
    private InvestmentAssetRepository investmentAssetRepository;
    @Mock
    private InvestmentValuationRepository investmentValuationRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;

    private InvestmentOperationService service() {
        InvestmentAssetService assetService = new InvestmentAssetService(
                investmentAssetRepository, investmentOperationRepository, investmentValuationRepository,
                accountRepository, userRepository, new InvestmentPositionService(), new InvestmentAssetMapperImpl());
        return new InvestmentOperationService(
                investmentOperationRepository, assetService, new InvestmentPositionService(), new InvestmentAssetMapperImpl());
    }

    private InvestmentAsset asset() {
        return InvestmentAsset.builder().id(ASSET_ID).name("PETR4").type(AssetType.STOCK).active(true).build();
    }

    private InvestmentOperation buy(Long id, String quantity, String unitPrice, LocalDate date) {
        return InvestmentOperation.builder()
                .id(id).type(OperationType.BUY).quantity(new BigDecimal(quantity))
                .unitPrice(new BigDecimal(unitPrice)).operationDate(date).active(true).build();
    }

    @Test
    void buyOperationIncreasesPosition() {
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.of(asset()));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(ASSET_ID))
                .thenReturn(List.of());
        when(investmentOperationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentOperationResponse response = service().create(
                USER_ID, ASSET_ID, new CreateInvestmentOperationRequest(
                        OperationType.BUY, new BigDecimal("10"), new BigDecimal("50"), LocalDate.now()));

        assertThat(response.type()).isEqualTo(OperationType.BUY);
        assertThat(response.quantity()).isEqualByComparingTo("10");
    }

    @Test
    void sellOperationWithinCurrentQuantitySucceeds() {
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.of(asset()));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(ASSET_ID))
                .thenReturn(List.of(buy(1L, "10", "50", LocalDate.now().minusDays(1))));
        when(investmentOperationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentOperationResponse response = service().create(
                USER_ID, ASSET_ID, new CreateInvestmentOperationRequest(
                        OperationType.SELL, new BigDecimal("4"), new BigDecimal("60"), LocalDate.now()));

        assertThat(response.type()).isEqualTo(OperationType.SELL);
    }

    @Test
    void sellOperationExceedingCurrentQuantityIsRejected() {
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.of(asset()));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(ASSET_ID))
                .thenReturn(List.of(buy(1L, "10", "50", LocalDate.now().minusDays(1))));

        assertThatThrownBy(() -> service().create(
                USER_ID, ASSET_ID, new CreateInvestmentOperationRequest(
                        OperationType.SELL, new BigDecimal("11"), new BigDecimal("60"), LocalDate.now())))
                .isInstanceOf(InvalidInvestmentDataException.class);

        verify(investmentOperationRepository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesOperation() {
        InvestmentOperation operation = buy(5L, "10", "50", LocalDate.now());
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.of(asset()));
        when(investmentOperationRepository.findByIdAndAssetId(5L, ASSET_ID)).thenReturn(Optional.of(operation));
        when(investmentOperationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().delete(USER_ID, ASSET_ID, 5L);

        assertThat(operation.isActive()).isFalse();
    }

    @Test
    void operationNeverTouchesAccountRepository() {
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.of(asset()));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(ASSET_ID))
                .thenReturn(List.of());
        when(investmentOperationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().create(USER_ID, ASSET_ID, new CreateInvestmentOperationRequest(
                OperationType.BUY, new BigDecimal("10"), new BigDecimal("50"), LocalDate.now()));

        verify(accountRepository, never()).save(any());
        verify(accountRepository, never()).findByIdForUpdate(any());
    }
}
