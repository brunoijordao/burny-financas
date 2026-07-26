package com.burny.financas.investments.service;

import com.burny.financas.investments.dto.CreateInvestmentOperationRequest;
import com.burny.financas.investments.dto.InvestmentOperationResponse;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentOperation;
import com.burny.financas.investments.entity.OperationType;
import com.burny.financas.investments.exception.InvalidInvestmentDataException;
import com.burny.financas.investments.exception.InvestmentAssetNotFoundException;
import com.burny.financas.investments.mapper.InvestmentAssetMapper;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buy/sell operations never touch {@code Account} balance or create a {@code Transaction}
 * (design.md Decision 4) — this service only ever reads/writes {@link InvestmentOperation} rows.
 */
@Service
@RequiredArgsConstructor
public class InvestmentOperationService {

    private final InvestmentOperationRepository investmentOperationRepository;
    private final InvestmentAssetService investmentAssetService;
    private final InvestmentPositionService investmentPositionService;
    private final InvestmentAssetMapper investmentAssetMapper;

    @Transactional
    public InvestmentOperationResponse create(Long userId, Long assetId, CreateInvestmentOperationRequest request) {
        InvestmentAsset asset = investmentAssetService.findOwnedOrThrow(userId, assetId);

        List<InvestmentOperation> existingOperations =
                investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(assetId);

        if (request.type() == OperationType.SELL) {
            AssetPosition currentPosition = investmentPositionService.calculate(existingOperations);
            if (request.quantity().compareTo(currentPosition.quantity()) > 0) {
                throw new InvalidInvestmentDataException("Sell quantity exceeds the asset's current quantity");
            }
        }

        InvestmentOperation operation = InvestmentOperation.builder()
                .asset(asset)
                .type(request.type())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .operationDate(request.operationDate())
                .active(true)
                .build();
        return investmentAssetMapper.toResponse(investmentOperationRepository.save(operation));
    }

    @Transactional(readOnly = true)
    public List<InvestmentOperationResponse> list(Long userId, Long assetId) {
        investmentAssetService.findOwnedOrThrow(userId, assetId);
        return investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(assetId).stream()
                .map(investmentAssetMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long assetId, Long operationId) {
        investmentAssetService.findOwnedOrThrow(userId, assetId);
        InvestmentOperation operation = investmentOperationRepository.findByIdAndAssetId(operationId, assetId)
                .orElseThrow(() -> new InvestmentAssetNotFoundException("Investment operation not found"));
        operation.setActive(false);
        investmentOperationRepository.save(operation);
    }
}
