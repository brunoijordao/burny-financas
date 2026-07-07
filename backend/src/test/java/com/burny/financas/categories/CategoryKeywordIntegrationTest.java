package com.burny.financas.categories;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class CategoryKeywordIntegrationTest {

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

    private long createCategory(String authHeader, String name) throws Exception {
        String response = mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"icon\":\"utensils\",\"color\":\"#000\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void successfulKeywordRegistration() throws Exception {
        String auth = authHeaderFor("keyword-create@example.com");
        long categoryId = createCategory(auth, "Alimentação");

        mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyword", is("IFOOD")))
                .andExpect(jsonPath("$.categoryId", is((int) categoryId)));
    }

    @Test
    void cannotRegisterKeywordOnAnotherUsersCategory() throws Exception {
        String ownerAuth = authHeaderFor("keyword-owner@example.com");
        long categoryId = createCategory(ownerAuth, "Alimentação");

        String otherAuth = authHeaderFor("keyword-other@example.com");

        mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void multipleKeywordsPerCategory() throws Exception {
        String auth = authHeaderFor("keyword-multiple@example.com");
        long categoryId = createCategory(auth, "Alimentação");

        mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"RAPPI\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/categories/" + categoryId + "/keywords").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void duplicateKeywordForSameUserRejectedWith409() throws Exception {
        String auth = authHeaderFor("keyword-duplicate@example.com");
        long foodCategoryId = createCategory(auth, "Alimentação");
        long leisureCategoryId = createCategory(auth, "Lazer");

        mockMvc.perform(post("/categories/" + foodCategoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/categories/" + leisureCategoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"ifood\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void sameKeywordAllowedForDifferentUsers() throws Exception {
        String firstAuth = authHeaderFor("keyword-cross-user-1@example.com");
        long firstCategoryId = createCategory(firstAuth, "Alimentação");
        mockMvc.perform(post("/categories/" + firstCategoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, firstAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated());

        String secondAuth = authHeaderFor("keyword-cross-user-2@example.com");
        long secondCategoryId = createCategory(secondAuth, "Alimentação");
        mockMvc.perform(post("/categories/" + secondCategoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, secondAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void listingKeywordsOnlyReturnsCallersOwn() throws Exception {
        String auth = authHeaderFor("keyword-list-own@example.com");
        long categoryId = createCategory(auth, "Alimentação");
        mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated());

        String otherAuth = authHeaderFor("keyword-list-other@example.com");

        mockMvc.perform(get("/categories/" + categoryId + "/keywords").header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulKeywordRemoval() throws Exception {
        String auth = authHeaderFor("keyword-remove@example.com");
        long categoryId = createCategory(auth, "Alimentação");
        String response = mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long keywordId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/categories/" + categoryId + "/keywords/" + keywordId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories/" + categoryId + "/keywords").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void cannotRemoveAnotherUsersKeyword() throws Exception {
        String ownerAuth = authHeaderFor("keyword-remove-owner@example.com");
        long categoryId = createCategory(ownerAuth, "Alimentação");
        String response = mockMvc.perform(post("/categories/" + categoryId + "/keywords")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"IFOOD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long keywordId = objectMapper.readTree(response).get("id").asLong();

        String otherAuth = authHeaderFor("keyword-remove-other@example.com");

        mockMvc.perform(delete("/categories/" + categoryId + "/keywords/" + keywordId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }
}
