package com.burny.financas.categories.service;

import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.categories.dto.CategoryKeywordResponse;
import com.burny.financas.categories.dto.CreateCategoryKeywordRequest;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.entity.CategoryKeyword;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.exception.DuplicateKeywordException;
import com.burny.financas.categories.mapper.CategoryMapper;
import com.burny.financas.categories.repository.CategoryKeywordRepository;
import com.burny.financas.categories.repository.CategoryRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryKeywordService {

    private final CategoryKeywordRepository categoryKeywordRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryKeywordResponse create(Long userId, Long categoryId, CreateCategoryKeywordRequest request) {
        Category category = findOwnedCategoryOrThrow(categoryId, userId);

        String normalized = request.keyword().trim().toUpperCase(Locale.ROOT);
        if (categoryKeywordRepository.existsByUserIdAndKeywordNormalized(userId, normalized)) {
            throw new DuplicateKeywordException("Keyword is already associated with one of your categories");
        }

        CategoryKeyword keyword = CategoryKeyword.builder()
                .user(userRepository.getReferenceById(userId))
                .category(category)
                .keyword(request.keyword().trim())
                .keywordNormalized(normalized)
                .build();

        return categoryMapper.toResponse(categoryKeywordRepository.save(keyword));
    }

    @Transactional(readOnly = true)
    public List<CategoryKeywordResponse> list(Long userId, Long categoryId) {
        findOwnedCategoryOrThrow(categoryId, userId);
        return categoryKeywordRepository.findAllByCategoryIdAndUserId(categoryId, userId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long categoryId, Long keywordId) {
        findOwnedCategoryOrThrow(categoryId, userId);
        CategoryKeyword keyword = categoryKeywordRepository.findByIdAndUserId(keywordId, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Keyword not found"));
        categoryKeywordRepository.delete(keyword);
    }

    private Category findOwnedCategoryOrThrow(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }
}
