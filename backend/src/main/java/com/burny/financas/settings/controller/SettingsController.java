package com.burny.financas.settings.controller;

import com.burny.financas.settings.dto.UpdateUserPreferencesRequest;
import com.burny.financas.settings.dto.UserPreferencesResponse;
import com.burny.financas.settings.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Settings", description = "Per-user display preferences (currency and date format)")
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserPreferencesService userPreferencesService;

    @Operation(summary = "Get the authenticated user's display preferences, defaulting to BRL / DD/MM/YYYY if never saved")
    @GetMapping("/preferences")
    public UserPreferencesResponse getPreferences(Authentication authentication) {
        return userPreferencesService.get(currentUserId(authentication));
    }

    @Operation(summary = "Update (create or overwrite) the authenticated user's display preferences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences saved"),
            @ApiResponse(responseCode = "400", description = "Unsupported currency or date format")
    })
    @PutMapping("/preferences")
    public UserPreferencesResponse updatePreferences(
            @Valid @RequestBody UpdateUserPreferencesRequest request,
            Authentication authentication
    ) {
        return userPreferencesService.update(currentUserId(authentication), request);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
