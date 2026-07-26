package com.burny.financas.investments.repository;

import com.burny.financas.investments.entity.InvestmentOperation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentOperationRepository extends JpaRepository<InvestmentOperation, Long> {

    Optional<InvestmentOperation> findByIdAndAssetId(Long id, Long assetId);

    List<InvestmentOperation> findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(Long assetId);
}
