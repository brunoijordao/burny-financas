package com.burny.financas.investments.repository;

import com.burny.financas.investments.entity.InvestmentValuation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentValuationRepository extends JpaRepository<InvestmentValuation, Long> {

    List<InvestmentValuation> findAllByAssetIdAndActiveTrueOrderByValueDateAsc(Long assetId);

    Optional<InvestmentValuation> findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(Long assetId);

    /** Used for net worth evolution and portfolio-total-as-of computations across a user's own assets. */
    List<InvestmentValuation> findAllByAssetIdInAndActiveTrueOrderByValueDateAsc(List<Long> assetIds);
}
