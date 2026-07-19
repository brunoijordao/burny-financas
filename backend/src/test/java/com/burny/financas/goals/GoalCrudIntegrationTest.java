package com.burny.financas.goals;

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
import java.time.LocalDate;
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
class GoalCrudIntegrationTest {

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

    private String goalBody(String name, String targetAmount, LocalDate deadline) {
        return "{\"name\":\"" + name + "\",\"targetAmount\":" + targetAmount + ",\"deadline\":\"" + deadline + "\"}";
    }

    private long createGoal(String authHeader, String name, String targetAmount, LocalDate deadline) throws Exception {
        String response = mockMvc.perform(post("/goals")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody(name, targetAmount, deadline)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void creationWithPositiveTargetSucceeds() throws Exception {
        String auth = authHeaderFor("goal-create@example.com");
        LocalDate deadline = LocalDate.now().plusMonths(6);

        mockMvc.perform(post("/goals")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Viagem", "5000", deadline)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Viagem")))
                .andExpect(jsonPath("$.targetAmount", is(5000)))
                .andExpect(jsonPath("$.currentAmount", is(0)))
                .andExpect(jsonPath("$.completed", is(false)));
    }

    @Test
    void nonPositiveTargetIsRejected() throws Exception {
        String auth = authHeaderFor("goal-invalid-target@example.com");
        LocalDate deadline = LocalDate.now().plusMonths(6);

        mockMvc.perform(post("/goals")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Viagem", "0", deadline)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCanRetrieveTheirGoal() throws Exception {
        String auth = authHeaderFor("goal-owner-get@example.com");
        long goalId = createGoal(auth, "Viagem", "5000", LocalDate.now().plusMonths(6));

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) goalId)));
    }

    @Test
    void nonOwnerCannotRetrieveAnotherUsersGoal() throws Exception {
        String ownerAuth = authHeaderFor("goal-isolation-owner@example.com");
        long goalId = createGoal(ownerAuth, "Viagem", "5000", LocalDate.now().plusMonths(6));

        String otherAuth = authHeaderFor("goal-isolation-other@example.com");

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingOnlyReturnsCallersOwnGoals() throws Exception {
        String auth = authHeaderFor("goal-list-own@example.com");
        createGoal(auth, "Minha", "5000", LocalDate.now().plusMonths(6));

        String otherAuth = authHeaderFor("goal-list-other@example.com");
        createGoal(otherAuth, "Alheia", "3000", LocalDate.now().plusMonths(6));

        mockMvc.perform(get("/goals").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("Minha")));
    }

    @Test
    void ownerCanUpdateTheirGoal() throws Exception {
        String auth = authHeaderFor("goal-update-owner@example.com");
        long goalId = createGoal(auth, "Viagem", "5000", LocalDate.now().plusMonths(6));
        LocalDate newDeadline = LocalDate.now().plusMonths(9);

        mockMvc.perform(put("/goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Viagem Internacional", "6000", newDeadline)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Viagem Internacional")))
                .andExpect(jsonPath("$.targetAmount", is(6000)));
    }

    @Test
    void nonOwnerCannotUpdateAnotherUsersGoal() throws Exception {
        String ownerAuth = authHeaderFor("goal-update-owner2@example.com");
        long goalId = createGoal(ownerAuth, "Viagem", "5000", LocalDate.now().plusMonths(6));

        String otherAuth = authHeaderFor("goal-update-other@example.com");

        mockMvc.perform(put("/goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Hacked", "1", LocalDate.now().plusMonths(6))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAGoalSoftDeletesItAndExcludesFromListing() throws Exception {
        String auth = authHeaderFor("goal-delete@example.com");
        long goalId = createGoal(auth, "Viagem", "5000", LocalDate.now().plusMonths(6));

        mockMvc.perform(delete("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/goals").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void nonOwnerCannotDeleteAnotherUsersGoal() throws Exception {
        String ownerAuth = authHeaderFor("goal-delete-owner@example.com");
        long goalId = createGoal(ownerAuth, "Viagem", "5000", LocalDate.now().plusMonths(6));

        String otherAuth = authHeaderFor("goal-delete-other@example.com");

        mockMvc.perform(delete("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, otherAuth))
                .andExpect(status().isNotFound());
    }
}
