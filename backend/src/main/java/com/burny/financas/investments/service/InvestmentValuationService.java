package com.burny.financas.investments.service;

import com.burny.financas.investments.dto.CreateInvestmentValuationRequest;
import com.burny.financas.investments.dto.InvestmentValuationResponse;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentValuation;
import com.burny.financas.investments.mapper.InvestmentAssetMapper;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every recorded valuation is retained as history — never overwritten (design.md Decision 2). */
@Service
@RequiredArgsConstructor
public class InvestmentValuationService {

    private final InvestmentValuationRepository investmentValuationRepository;
    private final InvestmentAssetService investmentAssetService;
    private final InvestmentAssetMapper investmentAssetMapper;

    @Transactional
    public InvestmentValuationResponse create(Long userId, Long assetId, CreateInvestmentValuationRequest request) {
        InvestmentAsset asset = investmentAssetService.findOwnedOrThrow(userId, assetId);

        InvestmentValuation valuation = InvestmentValuation.builder()
                .asset(asset)
                .valueDate(request.valueDate())
                .totalValue(request.totalValue())
                .active(true)
                .build();
        return investmentAssetMapper.toResponse(investmentValuationRepository.save(valuation));
    }

    @Transactional(readOnly = true)
    public List<InvestmentValuationResponse> list(Long userId, Long assetId) {
        investmentAssetService.findOwnedOrThrow(userId, assetId);
        return investmentValuationRepository.findAllByAssetIdAndActiveTrueOrderByValueDateAsc(assetId).stream()
                .map(investmentAssetMapper::toResponse)
                .toList();
    }
}
