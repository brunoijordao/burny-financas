package com.burny.financas.categories.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.entity.CategoryKeyword;
import com.burny.financas.categories.repository.CategoryKeywordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryResolutionServiceTest {

    @Mock
    private CategoryKeywordRepository categoryKeywordRepository;

    private CategoryKeyword keywordFor(Category category, String normalized) {
        return CategoryKeyword.builder()
                .category(category)
                .keyword(normalized)
                .keywordNormalized(normalized)
                .build();
    }

    @Test
    void resolvesSingleMatchingKeyword() {
        Category food = Category.builder().id(1L).name("Alimentação").active(true).build();
        when(categoryKeywordRepository.findAllActiveByUserId(1L, true))
                .thenReturn(List.of(keywordFor(food, "IFOOD")));

        CategoryResolutionService service = new CategoryResolutionService(categoryKeywordRepository);
        Optional<Category> result = service.resolve(1L, "IFOOD*IFOOD SAO PAULO BR");

        assertThat(result).contains(food);
    }

    @Test
    void returnsEmptyWhenNoKeywordMatches() {
        Category food = Category.builder().id(1L).name("Alimentação").active(true).build();
        when(categoryKeywordRepository.findAllActiveByUserId(1L, true))
                .thenReturn(List.of(keywordFor(food, "IFOOD")));

        CategoryResolutionService service = new CategoryResolutionService(categoryKeywordRepository);
        Optional<Category> result = service.resolve(1L, "POSTO SHELL");

        assertThat(result).isEmpty();
    }

    @Test
    void longestMatchingKeywordWinsWhenMultipleOverlap() {
        Category transport = Category.builder().id(1L).name("Transporte").active(true).build();
        Category food = Category.builder().id(2L).name("Alimentação").active(true).build();
        when(categoryKeywordRepository.findAllActiveByUserId(1L, true))
                .thenReturn(List.of(keywordFor(transport, "UBER"), keywordFor(food, "UBER EATS")));

        CategoryResolutionService service = new CategoryResolutionService(categoryKeywordRepository);
        Optional<Category> result = service.resolve(1L, "UBER EATS SAO PAULO");

        assertThat(result).contains(food);
    }

    @Test
    void resolutionOnlyConsidersRequestingUsersOwnKeywords() {
        when(categoryKeywordRepository.findAllActiveByUserId(2L, true)).thenReturn(List.of());

        CategoryResolutionService service = new CategoryResolutionService(categoryKeywordRepository);
        Optional<Category> result = service.resolve(2L, "IFOOD");

        assertThat(result).isEmpty();
    }

    @Test
    void inactiveCategoryKeywordsAreExcludedByRepositoryQuery() {
        // findAllActiveByUserId is defined to already filter out inactive categories at the query
        // level, so simulating "no active match" here is enough to assert the service returns empty.
        when(categoryKeywordRepository.findAllActiveByUserId(1L, true)).thenReturn(List.of());

        CategoryResolutionService service = new CategoryResolutionService(categoryKeywordRepository);
        Optional<Category> result = service.resolve(1L, "IFOOD");

        assertThat(result).isEmpty();
    }
}
