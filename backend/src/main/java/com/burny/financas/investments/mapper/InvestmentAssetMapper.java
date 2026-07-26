package com.burny.financas.investments.mapper;

import com.burny.financas.investments.dto.InvestmentAssetResponse;
import com.burny.financas.investments.dto.InvestmentOperationResponse;
import com.burny.financas.investments.dto.InvestmentValuationResponse;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentOperation;
import com.burny.financas.investments.entity.InvestmentValuation;
import com.burny.financas.investments.service.AssetPosition;
import java.math.BigDecimal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvestmentAssetMapper {

    /** Position and profitability fields are all computed per request (see the service layer), never persisted columns. */
    default InvestmentAssetResponse toResponse(
            InvestmentAsset asset,
            AssetPosition position,
            BigDecimal currentValue,
            BigDecimal profitabilityAmount,
            BigDecimal profitabilityPercentage
    ) {
        return new InvestmentAssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getTicker(),
                asset.getType(),
                asset.getAccount() != null ? asset.getAccount().getId() : null,
                asset.getAccount() != null ? asset.getAccount().getName() : null,
                position.quantity(),
                position.averagePrice(),
                position.investedAmount(),
                currentValue,
                profitabilityAmount,
                profitabilityPercentage,
                asset.isActive(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }

    default InvestmentOperationResponse toResponse(InvestmentOperation operation) {
        return new InvestmentOperationResponse(
                operation.getId(),
                operation.getAsset().getId(),
                operation.getType(),
                operation.getQuantity(),
                operation.getUnitPrice(),
                operation.getOperationDate(),
                operation.getCreatedAt());
    }

    default InvestmentValuationResponse toResponse(InvestmentValuation valuation) {
        return new InvestmentValuationResponse(
                valuation.getId(),
                valuation.getAsset().getId(),
                valuation.getValueDate(),
                valuation.getTotalValue(),
                valuation.getCreatedAt());
    }
}
