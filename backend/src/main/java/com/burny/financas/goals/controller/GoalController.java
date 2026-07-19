package com.burny.financas.goals.controller;

import com.burny.financas.goals.dto.ContributionResponse;
import com.burny.financas.goals.dto.CreateContributionRequest;
import com.burny.financas.goals.dto.CreateGoalRequest;
import com.burny.financas.goals.dto.GoalResponse;
import com.burny.financas.goals.dto.UpdateGoalRequest;
import com.burny.financas.goals.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goals", description = "Savings goals with a manual contribution ledger, computed progress, and pace projection")
@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @Operation(summary = "Create a savings goal owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Goal created"),
            @ApiResponse(responseCode = "400", description = "Invalid goal data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody CreateGoalRequest request, Authentication authentication) {
        return goalService.create(currentUserId(authentication), request);
    }

    @Operation(summary = "List the authenticated user's active goals, including completed ones, with computed progress")
    @GetMapping
    public List<GoalResponse> list(Authentication authentication) {
        return goalService.list(currentUserId(authentication));
    }

    @Operation(summary = "Get a single goal owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goal found"),
            @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable Long id, Authentication authentication) {
        return goalService.get(currentUserId(authentication), id);
    }

    @Operation(summary = "Edit a goal owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goal updated"),
            @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller")
    })
    @PutMapping("/{id}")
    public GoalResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication
    ) {
        return goalService.update(currentUserId(authentication), id, request);
    }

    @Operation(summary = "Delete (soft-delete) a goal owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Goal deactivated"),
            @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        goalService.delete(currentUserId(authentication), id);
    }

    @Operation(summary = "Record a manual contribution against a goal owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contribution recorded"),
            @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller")
    })
    @PostMapping("/{id}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContributionResponse addContribution(
            @PathVariable Long id,
            @Valid @RequestBody CreateContributionRequest request,
            Authentication authentication
    ) {
        return goalService.addContribution(currentUserId(authentication), id, request.amount(), request.contributionDate());
    }

    @Operation(summary = "List the contributions recorded against a goal owned by the authenticated user")
    @GetMapping("/{id}/contributions")
    public List<ContributionResponse> listContributions(@PathVariable Long id, Authentication authentication) {
        return goalService.listContributions(currentUserId(authentication), id);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
