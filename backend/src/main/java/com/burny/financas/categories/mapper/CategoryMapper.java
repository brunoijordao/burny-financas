package com.burny.financas.categories.mapper;

import com.burny.financas.categories.dto.CategoryKeywordResponse;
import com.burny.financas.categories.dto.CategoryResponse;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.entity.CategoryKeyword;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    default CategoryResponse toResponse(Category category) {
        return toResponse(category, List.of());
    }

    /**
     * {@code subcategories} is mapped one level deep only (categories are capped at two levels, so
     * a subcategory is never itself given nested subcategories).
     */
    default CategoryResponse toResponse(Category category, List<Category> subcategories) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null,
                category.isDefaultCategory(),
                category.isActive(),
                subcategories.stream().map(sub -> toResponse(sub, List.of())).toList(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    default CategoryKeywordResponse toResponse(CategoryKeyword keyword) {
        return new CategoryKeywordResponse(
                keyword.getId(),
                keyword.getCategory().getId(),
                keyword.getKeyword(),
                keyword.getCreatedAt()
        );
    }
}
