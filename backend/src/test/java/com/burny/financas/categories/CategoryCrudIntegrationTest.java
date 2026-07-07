package com.burny.financas.categories;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CategoryCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private long createCategory(String authHeader, String name, Long parentCategoryId) throws Exception {
        String body = "{"
                + "\"name\":\"" + name + "\","
                + "\"icon\":\"utensils\","
                + "\"color\":\"#123456\""
                + (parentCategoryId != null ? ",\"parentCategoryId\":" + parentCategoryId : "")
                + "}";
        String response = mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void successfulTopLevelCategoryCreation() throws Exception {
        String auth = authHeaderFor("category-create@example.com");

        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alimentação\",\"icon\":\"utensils\",\"color\":\"#f97316\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alimentação")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.defaultCategory", is(false)))
                .andExpect(jsonPath("$.parentCategoryId").doesNotExist());
    }

    @Test
    void categoryCreationWithoutNameRejectedWith400() throws Exception {
        String auth = authHeaderFor("category-no-name@example.com");

        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"icon\":\"utensils\",\"color\":\"#f97316\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void successfulSubcategoryCreation() throws Exception {
        String auth = authHeaderFor("subcategory-create@example.com");
        long parentId = createCategory(auth, "Alimentação", null);

        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurantes\",\"icon\":\"utensils\",\"color\":\"#000\",\"parentCategoryId\":" + parentId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentCategoryId", is((int) parentId)));
    }

    @Test
    void subcategoryCreationWithParentFromAnotherUserRejectedWith404() throws Exception {
        String ownerAuth = authHeaderFor("subcategory-parent-owner@example.com");
        long parentId = createCategory(ownerAuth, "Alimentação", null);

        String otherAuth = authHeaderFor("subcategory-parent-other@example.com");

        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurantes\",\"icon\":\"x\",\"color\":\"#000\",\"parentCategoryId\":" + parentId + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void subcategoryCannotBeUsedAsParentRejectedWith422() throws Exception {
        String auth = authHeaderFor("subcategory-depth@example.com");
        long parentId = createCategory(auth, "Alimentação", null);
        long subcategoryId = createCategory(auth, "Restaurantes", parentId);

        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Delivery\",\"icon\":\"x\",\"color\":\"#000\",\"parentCategoryId\":" + subcategoryId + "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void nonexistentParentCategoryRejectedWith404() throws Exception {
        String auth = authHeaderFor("subcategory-missing-parent@example.com");

        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurantes\",\"icon\":\"x\",\"color\":\"#000\",\"parentCategoryId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanRetrieveTheirCategory() throws Exception {
        String auth = authHeaderFor("category-owner-get@example.com");
        long id = createCategory(auth, "Transporte", null);

        mockMvc.perform(get("/categories/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)));
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersCategory() throws Exception {
        String ownerAuth = authHeaderFor("category-isolation-owner@example.com");
        long id = createCategory(ownerAuth, "Transporte", null);

        String otherAuth = authHeaderFor("category-isolation-other@example.com");

        mockMvc.perform(get("/categories/" + id).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnCategoriesWithNestedSubcategories() throws Exception {
        // Note: registration auto-provisions 5 default categories (see
        // specs/categories/spec.md "Default Categories Provisioned On Registration"), so this
        // asserts on the caller's custom category specifically rather than the full list length.
        String auth = authHeaderFor("category-list-own@example.com");
        long parentId = createCategory(auth, "Compras", null);
        createCategory(auth, "Roupas", parentId);

        String otherAuth = authHeaderFor("category-list-other@example.com");
        createCategory(otherAuth, "Conta Alheia", null);

        mockMvc.perform(get("/categories").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Compras')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Compras')].subcategories[0].name", org.hamcrest.Matchers.contains("Roupas")))
                .andExpect(jsonPath("$[?(@.name == 'Conta Alheia')]").isEmpty());
    }

    @Test
    void successfulEditUpdatesNameIconAndColor() throws Exception {
        String auth = authHeaderFor("category-edit-success@example.com");
        long id = createCategory(auth, "Old Name", null);

        mockMvc.perform(put("/categories/" + id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"icon\":\"new-icon\",\"color\":\"#FFFFFF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.icon", is("new-icon")));
    }

    @Test
    void cannotEditAnotherUsersCategory() throws Exception {
        String ownerAuth = authHeaderFor("category-edit-owner@example.com");
        long id = createCategory(ownerAuth, "Categoria", null);

        String otherAuth = authHeaderFor("category-edit-other@example.com");

        mockMvc.perform(put("/categories/" + id)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"icon\":\"x\",\"color\":\"#000\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void editRequestCannotChangeParentCategory() throws Exception {
        String auth = authHeaderFor("category-edit-parent@example.com");
        long parentId = createCategory(auth, "Alimentação", null);
        long otherParentId = createCategory(auth, "Transporte", null);
        long subcategoryId = createCategory(auth, "Restaurantes", parentId);

        // parentCategoryId isn't a field on UpdateCategoryRequest, so sending it is a no-op.
        mockMvc.perform(put("/categories/" + subcategoryId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Restaurantes\",\"icon\":\"x\",\"color\":\"#000\",\"parentCategoryId\":" + otherParentId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentCategoryId", is((int) parentId)));
    }

    @Test
    void deletingTopLevelCategorySoftDeletesIt() throws Exception {
        String auth = authHeaderFor("category-delete@example.com");
        long id = createCategory(auth, "Categoria", null);

        mockMvc.perform(delete("/categories/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deletingTopLevelCategoryCascadesToSubcategories() throws Exception {
        String auth = authHeaderFor("category-delete-cascade@example.com");
        long parentId = createCategory(auth, "Alimentação", null);
        long subcategoryId = createCategory(auth, "Restaurantes", parentId);

        mockMvc.perform(delete("/categories/" + parentId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories/" + subcategoryId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deletingSubcategoryDoesNotAffectParent() throws Exception {
        String auth = authHeaderFor("category-delete-sub-only@example.com");
        long parentId = createCategory(auth, "Alimentação", null);
        long subcategoryId = createCategory(auth, "Restaurantes", parentId);

        mockMvc.perform(delete("/categories/" + subcategoryId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories/" + parentId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void cannotDeleteAnotherUsersCategory() throws Exception {
        String ownerAuth = authHeaderFor("category-delete-owner@example.com");
        long id = createCategory(ownerAuth, "Categoria", null);

        String otherAuth = authHeaderFor("category-delete-other@example.com");

        mockMvc.perform(delete("/categories/" + id).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveCategoriesExcludedFromListings() throws Exception {
        String auth = authHeaderFor("category-list-excludes-inactive@example.com");
        long id = createCategory(auth, "Categoria", null);

        mockMvc.perform(delete("/categories/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }
}
