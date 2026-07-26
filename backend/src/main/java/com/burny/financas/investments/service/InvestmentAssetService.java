package com.burny.financas.investments.service;

import com.burny.financas.accounts.entity.Account;
import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.investments.dto.CreateInvestmentAssetRequest;
import com.burny.financas.investments.dto.InvestmentAssetResponse;
import com.burny.financas.investments.dto.UpdateInvestmentAssetRequest;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentValuation;
import com.burny.financas.investments.exception.InvalidInvestmentDataException;
import com.burny.financas.investments.exception.InvestmentAssetNotFoundException;
import com.burny.financas.investments.mapper.InvestmentAssetMapper;
import com.burny.financas.investments.repository.InvestmentAssetRepository;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code account} is validated for ownership on create/update but never written to (design.md
 * Decision 4) — this service never touches {@code AccountRepository} beyond that read, and never
 * calls {@code TransactionBalanceService}.
 */
@Service
@RequiredArgsConstructor
public class InvestmentAssetService {

    private final InvestmentAssetRepository investmentAssetRepository;
    private final InvestmentOperationRepository investmentOperationRepository;
    private final InvestmentValuationRepository investmentValuationRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final InvestmentPositionService investmentPositionService;
    private final InvestmentAssetMapper investmentAssetMapper;

    @Transactional
    public InvestmentAssetResponse create(Long userId, CreateInvestmentAssetRequest request) {
        Account account = resolveAccount(userId, request.accountId());
        InvestmentAsset asset = InvestmentAsset.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .name(request.name())
                .ticker(request.ticker())
                .type(request.type())
                .active(true)
                .build();
        return toResponse(investmentAssetRepository.save(asset));
    }

    @Transactional
    public InvestmentAssetResponse update(Long userId, Long id, UpdateInvestmentAssetRequest request) {
        InvestmentAsset asset = findOwnedOrThrow(userId, id);
        Account account = resolveAccount(userId, request.accountId());
        asset.setName(request.name());
        asset.setTicker(request.ticker());
        asset.setType(request.type());
        asset.setAccount(account);
        return toResponse(investmentAssetRepository.save(asset));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        InvestmentAsset asset = findOwnedOrThrow(userId, id);
        asset.setActive(false);
        investmentAssetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public InvestmentAssetResponse get(Long userId, Long id) {
        return toResponse(findOwnedOrThrow(userId, id));
    }

    @Transactional(readOnly = true)
    public List<InvestmentAssetResponse> list(Long userId) {
        return investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    InvestmentAsset findOwnedOrThrow(Long userId, Long id) {
        return investmentAssetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new InvestmentAssetNotFoundException("Investment asset not found"));
    }

    AssetPosition getPosition(Long assetId) {
        return investmentPositionService.calculate(
                investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(assetId));
    }

    private InvestmentAssetResponse toResponse(InvestmentAsset asset) {
        AssetPosition position = getPosition(asset.getId());
        BigDecimal currentValue = investmentValuationRepository
                .findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(asset.getId())
                .map(InvestmentValuation::getTotalValue)
                .orElse(null);

        BigDecimal profitabilityAmount = null;
        BigDecimal profitabilityPercentage = null;
        if (currentValue != null) {
            profitabilityAmount = currentValue.subtract(position.investedAmount());
            profitabilityPercentage = position.investedAmount().signum() > 0
                    ? profitabilityAmount.multiply(BigDecimal.valueOf(100))
                            .divide(position.investedAmount(), 2, RoundingMode.HALF_UP)
                    : null;
        }

        return investmentAssetMapper.toResponse(asset, position, currentValue, profitabilityAmount, profitabilityPercentage);
    }

    private Account resolveAccount(Long userId, Long accountId) {
        if (accountId == null) {
            return null;
        }
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new InvalidInvestmentDataException("Account not found or not owned by the caller"));
    }
}
