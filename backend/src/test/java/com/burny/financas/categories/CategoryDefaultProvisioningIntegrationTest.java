package com.burny.financas.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.entity.DefaultCategory;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.categories.repository.DefaultCategoryRepository;
import com.burny.financas.categories.service.DefaultCategoryProvisioningService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryDefaultProvisioningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DefaultCategoryRepository defaultCategoryRepository;

    @Autowired
    private DefaultCategoryProvisioningService provisioningService;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    @Test
    void newUserReceivesTheCurrentlyActiveDefaultCategories() throws Exception {
        List<String> expectedNames = defaultCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(DefaultCategory::getName)
                .toList();

        String auth = authHeaderFor("defaults-new-user@example.com");

        mockMvc.perform(get("/categories").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(expectedNames.size())))
                .andExpect(jsonPath("$[*].defaultCategory", org.hamcrest.Matchers.everyItem(is(true))));

        List<String> names = categoryRepository
                .findAllByUserIdAndActiveTrueAndParentCategoryIsNull(
                        userRepository.findByEmail("defaults-new-user@example.com").orElseThrow().getId())
                .stream()
                .map(Category::getName)
                .toList();
        assertThat(names).containsExactlyInAnyOrderElementsOf(expectedNames);
    }

    @Test
    void editingDefaultCategoryDoesNotAffectOtherUsersCopy() throws Exception {
        String firstAuth = authHeaderFor("defaults-edit-first@example.com");
        String secondAuth = authHeaderFor("defaults-edit-second@example.com");

        String listResponse = mockMvc.perform(get("/categories").header(HttpHeaders.AUTHORIZATION, firstAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long firstUserFoodCategoryId = extractIdByName(listResponse, "Alimentação");

        mockMvc.perform(put("/categories/" + firstUserFoodCategoryId)
                        .header(HttpHeaders.AUTHORIZATION, firstAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Comida\",\"icon\":\"utensils\",\"color\":\"#000\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/categories").header(HttpHeaders.AUTHORIZATION, secondAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Alimentação')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Comida')]").isEmpty());
    }

    @Test
    void provisioningIsIdempotent() {
        int expectedCount = defaultCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc().size();

        authService.register(new RegisterRequest("defaults-idempotent@example.com", "Password123"));
        Long userId = userRepository.findByEmail("defaults-idempotent@example.com").orElseThrow().getId();

        provisioningService.provisionDefaults(userId);
        provisioningService.provisionDefaults(userId);

        long defaultCount = categoryRepository.findAllByUserIdAndActiveTrueAndParentCategoryIsNull(userId).stream()
                .filter(category -> category.isDefaultCategory())
                .count();
        assertThat(defaultCount).isEqualTo(expectedCount);
    }

    @Test
    void deactivatedDefaultCategoryRowIsNotProvisioned() {
        DefaultCategory transporte = defaultCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .filter(row -> row.getName().equals("Transporte"))
                .findFirst()
                .orElseThrow();

        transporte.setActive(false);
        defaultCategoryRepository.save(transporte);
        try {
            authService.register(new RegisterRequest("defaults-deactivated-row@example.com", "Password123"));
            Long userId = userRepository.findByEmail("defaults-deactivated-row@example.com").orElseThrow().getId();

            List<String> names = categoryRepository.findAllByUserIdAndActiveTrueAndParentCategoryIsNull(userId)
                    .stream()
                    .map(Category::getName)
                    .toList();

            assertThat(names).doesNotContain("Transporte");
        } finally {
            transporte.setActive(true);
            defaultCategoryRepository.save(transporte);
        }
    }

    @Test
    void provisionedCategoriesFollowConfiguredSortOrder() {
        List<DefaultCategory> original = defaultCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc();
        DefaultCategory first = original.get(0);
        DefaultCategory second = original.get(1);
        int firstOriginalSortOrder = first.getSortOrder();
        int secondOriginalSortOrder = second.getSortOrder();

        // Swap the sort order of the first two active rows and confirm provisioning follows suit.
        first.setSortOrder(secondOriginalSortOrder);
        second.setSortOrder(firstOriginalSortOrder);
        defaultCategoryRepository.save(first);
        defaultCategoryRepository.save(second);
        try {
            authService.register(new RegisterRequest("defaults-sort-order@example.com", "Password123"));
            Long userId = userRepository.findByEmail("defaults-sort-order@example.com").orElseThrow().getId();

            List<String> provisionedNames = categoryRepository
                    .findAllByUserIdAndActiveTrueAndParentCategoryIsNull(userId).stream()
                    .sorted((a, b) -> a.getId().compareTo(b.getId()))
                    .map(Category::getName)
                    .toList();
            List<String> expectedOrder = defaultCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()
                    .stream()
                    .map(DefaultCategory::getName)
                    .toList();

            assertThat(provisionedNames).containsExactlyElementsOf(expectedOrder);
            assertThat(provisionedNames.get(0)).isEqualTo(second.getName());
            assertThat(provisionedNames.get(1)).isEqualTo(first.getName());
        } finally {
            first.setSortOrder(firstOriginalSortOrder);
            second.setSortOrder(secondOriginalSortOrder);
            defaultCategoryRepository.save(first);
            defaultCategoryRepository.save(second);
        }
    }

    private long extractIdByName(String jsonArray, String name) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonArray);
        for (com.fasterxml.jackson.databind.JsonNode node : root) {
            if (node.get("name").asText().equals(name)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Category not found: " + name);
    }
}
