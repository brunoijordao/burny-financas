package com.burny.financas.investments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.investments.dto.CreateInvestmentValuationRequest;
import com.burny.financas.investments.dto.InvestmentValuationResponse;
import com.burny.financas.investments.entity.AssetType;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.exception.InvestmentAssetNotFoundException;
import com.burny.financas.investments.mapper.InvestmentAssetMapperImpl;
import com.burny.financas.investments.repository.InvestmentAssetRepository;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestmentValuationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ASSET_ID = 10L;

    @Mock
    private InvestmentValuationRepository investmentValuationRepository;
    @Mock
    private InvestmentAssetRepository investmentAssetRepository;
    @Mock
    private InvestmentOperationRepository investmentOperationRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;

    private InvestmentValuationService service() {
        InvestmentAssetService assetService = new InvestmentAssetService(
                investmentAssetRepository, investmentOperationRepository, investmentValuationRepository,
                accountRepository, userRepository, new InvestmentPositionService(), new InvestmentAssetMapperImpl());
        return new InvestmentValuationService(investmentValuationRepository, assetService, new InvestmentAssetMapperImpl());
    }

    private InvestmentAsset asset() {
        return InvestmentAsset.builder().id(ASSET_ID).name("PETR4").type(AssetType.STOCK).active(true).build();
    }

    @Test
    void recordingAValuationPersistsIt() {
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.of(asset()));
        when(investmentValuationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentValuationResponse response = service().create(
                USER_ID, ASSET_ID, new CreateInvestmentValuationRequest(LocalDate.now(), new BigDecimal("1300")));

        assertThat(response.totalValue()).isEqualByComparingTo("1300");
        assertThat(response.assetId()).isEqualTo(ASSET_ID);
    }

    @Test
    void cannotRecordValuationForAssetNotOwnedByCaller() {
        when(investmentAssetRepository.findByIdAndUserId(ASSET_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(
                USER_ID, ASSET_ID, new CreateInvestmentValuationRequest(LocalDate.now(), new BigDecimal("1300"))))
                .isInstanceOf(InvestmentAssetNotFoundException.class);
    }
}
