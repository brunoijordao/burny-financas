package com.burny.financas.categories.service;

import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.entity.CategoryKeyword;
import com.burny.financas.categories.repository.CategoryKeywordRepository;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal resolution operation for future use by transaction creation and PDF statement parsing
 * (see specs/category-auto-categorization-rules/spec.md "Category Resolution By Description"). Not
 * exposed as an HTTP endpoint in this change.
 */
@Service
@RequiredArgsConstructor
public class CategoryResolutionService {

    private final CategoryKeywordRepository categoryKeywordRepository;

    @Transactional(readOnly = true)
    public Optional<Category> resolve(Long userId, String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }

        String normalizedDescription = description.toUpperCase(Locale.ROOT);

        return categoryKeywordRepository.findAllActiveByUserId(userId, true).stream()
                .filter(keyword -> normalizedDescription.contains(keyword.getKeywordNormalized()))
                .max(Comparator.comparingInt(keyword -> keyword.getKeywordNormalized().length()))
                .map(CategoryKeyword::getCategory);
    }
}
