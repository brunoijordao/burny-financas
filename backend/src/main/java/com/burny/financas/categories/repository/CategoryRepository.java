package com.burny.financas.categories.repository;

import com.burny.financas.categories.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    List<Category> findAllByUserIdAndActiveTrueAndParentCategoryIsNull(Long userId);

    List<Category> findAllByUserIdAndActiveTrueAndParentCategoryIsNotNull(Long userId);

    List<Category> findAllByParentCategoryIdAndActiveTrue(Long parentCategoryId);

    boolean existsByUserIdAndDefaultCategoryTrue(Long userId);

    Optional<Category> findByUserIdAndNameIgnoreCaseAndActiveTrue(Long userId, String name);
}
