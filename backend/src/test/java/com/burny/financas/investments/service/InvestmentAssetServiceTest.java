package com.burny.financas.investments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.investments.dto.CreateInvestmentAssetRequest;
import com.burny.financas.investments.dto.InvestmentAssetResponse;
import com.burny.financas.investments.dto.UpdateInvestmentAssetRequest;
import com.burny.financas.investments.entity.AssetType;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.exception.InvalidInvestmentDataException;
import com.burny.financas.investments.exception.InvestmentAssetNotFoundException;
import com.burny.financas.investments.mapper.InvestmentAssetMapperImpl;
import com.burny.financas.investments.repository.InvestmentAssetRepository;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestmentAssetServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private InvestmentAssetRepository investmentAssetRepository;
    @Mock
    private InvestmentOperationRepository investmentOperationRepository;
    @Mock
    private InvestmentValuationRepository investmentValuationRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;

    private InvestmentAssetService service() {
        return new InvestmentAssetService(
                investmentAssetRepository, investmentOperationRepository, investmentValuationRepository,
                accountRepository, userRepository, new InvestmentPositionService(), new InvestmentAssetMapperImpl());
    }

    private InvestmentAsset asset(Long id, Account account) {
        return InvestmentAsset.builder()
                .id(id)
                .account(account)
                .name("PETR4")
                .type(AssetType.STOCK)
                .active(true)
                .build();
    }

    @Test
    void createWithoutAccountLinkSucceeds() {
        when(investmentAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentAssetResponse response = service().create(
                USER_ID, new CreateInvestmentAssetRequest("PETR4", "PETR4", AssetType.STOCK, null));

        assertThat(response.name()).isEqualTo("PETR4");
        assertThat(response.accountId()).isNull();
        assertThat(response.quantity()).isEqualByComparingTo("0");
    }

    @Test
    void createLinkedToOwnedAccountSucceeds() {
        Account account = Account.builder().id(2L).name("XP").build();
        when(accountRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(account));
        when(investmentAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentAssetResponse response = service().create(
                USER_ID, new CreateInvestmentAssetRequest("PETR4", "PETR4", AssetType.STOCK, 2L));

        assertThat(response.accountId()).isEqualTo(2L);
        assertThat(response.accountName()).isEqualTo("XP");
    }

    @Test
    void createRejectsAccountNotOwnedByCaller() {
        when(accountRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(
                USER_ID, new CreateInvestmentAssetRequest("PETR4", "PETR4", AssetType.STOCK, 2L)))
                .isInstanceOf(InvalidInvestmentDataException.class);
    }

    @Test
    void updateRejectsAssetNotOwnedByCaller() {
        when(investmentAssetRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(
                USER_ID, 10L, new UpdateInvestmentAssetRequest("PETR4", "PETR4", AssetType.STOCK, null)))
                .isInstanceOf(InvestmentAssetNotFoundException.class);
    }

    @Test
    void deleteSoftDeletesAssetPreservingIt() {
        InvestmentAsset existing = asset(10L, null);
        when(investmentAssetRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(existing));
        when(investmentAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().delete(USER_ID, 10L);

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void getRejectsAssetNotOwnedByCaller() {
        when(investmentAssetRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(USER_ID, 10L))
                .isInstanceOf(InvestmentAssetNotFoundException.class);
    }

    @Test
    void listOnlyReturnsCallersOwnActiveAssets() {
        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(asset(10L, null)));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(10L))
                .thenReturn(List.of());

        List<InvestmentAssetResponse> responses = service().list(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
    }

    @Test
    void assetWithNoValuationReportsUnknownCurrentValueAndNoProfitability() {
        when(investmentAssetRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(asset(10L, null)));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(10L))
                .thenReturn(List.of());
        when(investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(10L))
                .thenReturn(Optional.empty());

        InvestmentAssetResponse response = service().get(USER_ID, 10L);

        assertThat(response.currentValue()).isNull();
        assertThat(response.profitabilityAmount()).isNull();
        assertThat(response.profitabilityPercentage()).isNull();
    }
}
