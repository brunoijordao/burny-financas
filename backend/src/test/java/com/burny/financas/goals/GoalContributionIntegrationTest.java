package com.burny.financas.goals;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burny.financas.accounts.repository.AccountRepository;
import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.transactions.repository.TransactionRepository;
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
class GoalContributionIntegrationTest {

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

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String authHeaderFor(String email) {
        authService.register(new RegisterRequest(email, "Password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private long createGoal(String authHeader, String targetAmount, LocalDate deadline) throws Exception {
        String response = mockMvc.perform(post("/goals")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Viagem\",\"targetAmount\":" + targetAmount + ",\"deadline\":\"" + deadline + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void contribute(String authHeader, long goalId, String amount) throws Exception {
        mockMvc.perform(post("/goals/" + goalId + "/contributions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void contributionIncreasesCurrentAmount() throws Exception {
        String auth = authHeaderFor("goal-contrib-current@example.com");
        long goalId = createGoal(auth, "1000", LocalDate.now().plusMonths(6));

        contribute(auth, goalId, "200");

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount", is(200)));
    }

    @Test
    void contributionDoesNotTouchAnyAccountOrTransaction() throws Exception {
        String auth = authHeaderFor("goal-contrib-isolated@example.com");
        long goalId = createGoal(auth, "1000", LocalDate.now().plusMonths(6));

        long accountsBefore = accountRepository.count();
        long transactionsBefore = transactionRepository.count();

        contribute(auth, goalId, "200");

        org.assertj.core.api.Assertions.assertThat(accountRepository.count()).isEqualTo(accountsBefore);
        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(transactionsBefore);
    }

    @Test
    void nonOwnerCannotContributeToAnotherUsersGoal() throws Exception {
        String ownerAuth = authHeaderFor("goal-contrib-owner@example.com");
        long goalId = createGoal(ownerAuth, "1000", LocalDate.now().plusMonths(6));

        String otherAuth = authHeaderFor("goal-contrib-other@example.com");

        mockMvc.perform(post("/goals/" + goalId + "/contributions")
                        .header(HttpHeaders.AUTHORIZATION, otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void percentCompleteMatchesContributionsOverTarget() throws Exception {
        String auth = authHeaderFor("goal-contrib-percent@example.com");
        long goalId = createGoal(auth, "1000", LocalDate.now().plusMonths(6));

        contribute(auth, goalId, "250");

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentComplete", is(25.0)));
    }

    @Test
    void goalCompletesWhenTargetReachedAndStaysCompletedOnLaterRead() throws Exception {
        String auth = authHeaderFor("goal-contrib-complete@example.com");
        long goalId = createGoal(auth, "500", LocalDate.now().plusMonths(6));

        contribute(auth, goalId, "500");

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(true)));

        // A later read still reports completed.
        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    void completedGoalStillAppearsInGoalList() throws Exception {
        String auth = authHeaderFor("goal-contrib-history@example.com");
        long goalId = createGoal(auth, "500", LocalDate.now().plusMonths(6));

        contribute(auth, goalId, "500");

        mockMvc.perform(get("/goals").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].completed", is(true)));
    }

    @Test
    void noProjectedDateBeforeAnyContribution() throws Exception {
        String auth = authHeaderFor("goal-contrib-no-projection@example.com");
        long goalId = createGoal(auth, "1000", LocalDate.now().plusMonths(6));

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectedCompletionDate").doesNotExist())
                .andExpect(jsonPath("$.onTrack").doesNotExist());
    }

    @Test
    void projectionIsOnTrackWhenPaceWouldMeetDeadline() throws Exception {
        String auth = authHeaderFor("goal-contrib-on-track@example.com");
        // Target 5000, deadline far in the future; contributing 1000 on day 0 (clamped to a 1-day
        // pace) projects completion in ~4 days, comfortably inside a 60-day deadline.
        long goalId = createGoal(auth, "5000", LocalDate.now().plusDays(60));

        contribute(auth, goalId, "1000");

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onTrack", is(true)));
    }

    @Test
    void projectionIsNotOnTrackWhenPaceWouldMissDeadline() throws Exception {
        String auth = authHeaderFor("goal-contrib-off-track@example.com");
        // Target 5000, deadline tomorrow; a tiny contribution projects a completion date far past the deadline.
        long goalId = createGoal(auth, "5000", LocalDate.now().plusDays(1));

        contribute(auth, goalId, "10");

        mockMvc.perform(get("/goals/" + goalId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onTrack", is(false)));
    }
}
