package com.burny.financas.categories.repository;

import com.burny.financas.categories.entity.CategoryKeyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryKeywordRepository extends JpaRepository<CategoryKeyword, Long> {

    Optional<CategoryKeyword> findByIdAndUserId(Long id, Long userId);

    List<CategoryKeyword> findAllByCategoryIdAndUserId(Long categoryId, Long userId);

    boolean existsByUserIdAndKeywordNormalized(Long userId, String keywordNormalized);

    /**
     * Used by resolution: only keywords whose owning category is still active can match, per
     * "Resolution ignores keywords of inactive categories".
     *
     * <p>{@code c.active = true} is deliberately NOT written as a literal here: a hand-written JPQL
     * boolean literal compiles to an actual SQL {@code true} keyword, which H2's Oracle-compatibility
     * mode cannot compare against the NUMBER(1)-backed {@code active} column (unlike Spring Data's
     * derived {@code ...ActiveTrue} queries, which bind a parameter instead). Binding {@code true} as
     * a parameter avoids the literal entirely.
     */
    @Query("select k from CategoryKeyword k join fetch k.category c where k.user.id = :userId and c.active = :active")
    List<CategoryKeyword> findAllActiveByUserId(@Param("userId") Long userId, @Param("active") boolean active);
}
