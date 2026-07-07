package com.burny.financas.categories.service;

import com.burny.financas.categories.dto.CategoryResponse;
import com.burny.financas.categories.dto.CreateCategoryRequest;
import com.burny.financas.categories.dto.UpdateCategoryRequest;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.exception.InvalidCategoryHierarchyException;
import com.burny.financas.categories.mapper.CategoryMapper;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.auth.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(Long userId, CreateCategoryRequest request) {
        Category parent = null;
        if (request.parentCategoryId() != null) {
            parent = findOwnedOrThrow(request.parentCategoryId(), userId);
            if (!parent.isActive()) {
                throw new CategoryNotFoundException("Parent category not found");
            }
            if (parent.isSubcategory()) {
                throw new InvalidCategoryHierarchyException(
                        "A subcategory cannot be used as the parent of another category");
            }
        }

        Category category = Category.builder()
                .user(userRepository.getReferenceById(userId))
                .parentCategory(parent)
                .name(request.name())
                .icon(request.icon())
                .color(request.color())
                .defaultCategory(false)
                .active(true)
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        List<Category> topLevel = categoryRepository.findAllByUserIdAndActiveTrueAndParentCategoryIsNull(userId);
        List<Category> subcategories = categoryRepository.findAllByUserIdAndActiveTrueAndParentCategoryIsNotNull(userId);

        Map<Long, List<Category>> subcategoriesByParentId = subcategories.stream()
                .collect(Collectors.groupingBy(sub -> sub.getParentCategory().getId()));

        return topLevel.stream()
                .map(category -> categoryMapper.toResponse(
                        category, subcategoriesByParentId.getOrDefault(category.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long userId, Long id) {
        return categoryMapper.toResponse(findOwnedOrThrow(id, userId));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, UpdateCategoryRequest request) {
        Category category = findOwnedOrThrow(id, userId);

        category.setName(request.name());
        category.setIcon(request.icon());
        category.setColor(request.color());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Always a soft delete (see design.md "Soft delete via active boolean apenas"). Deactivating a
     * top-level category cascades to its still-active subcategories in the same transaction.
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Category category = findOwnedOrThrow(id, userId);
        category.setActive(false);
        categoryRepository.save(category);

        if (!category.isSubcategory()) {
            List<Category> children = categoryRepository.findAllByParentCategoryIdAndActiveTrue(category.getId());
            children.forEach(child -> child.setActive(false));
            categoryRepository.saveAll(children);
        }
    }

    private Category findOwnedOrThrow(Long id, Long userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }
}
