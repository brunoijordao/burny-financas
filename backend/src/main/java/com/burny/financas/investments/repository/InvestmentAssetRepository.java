package com.burny.financas.investments.repository;

import com.burny.financas.investments.entity.InvestmentAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every filter is a Spring Data derived query (the {@code ActiveTrue} suffix), which binds its
 * boolean as a parameter rather than inlining a JPQL literal, per the project's established
 * H2/Oracle convention (design.md Decision 6).
 */
public interface InvestmentAssetRepository extends JpaRepository<InvestmentAsset, Long> {

    Optional<InvestmentAsset> findByIdAndUserId(Long id, Long userId);

    List<InvestmentAsset> findAllByUserIdAndActiveTrueOrderByNameAsc(Long userId);
}
