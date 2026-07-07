package com.burny.financas.categories.repository;

import com.burny.financas.categories.entity.DefaultCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultCategoryRepository extends JpaRepository<DefaultCategory, Long> {

    List<DefaultCategory> findAllByActiveTrueOrderBySortOrderAscIdAsc();
}
