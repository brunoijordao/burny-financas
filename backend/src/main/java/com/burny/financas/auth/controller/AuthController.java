package com.burny.financas.auth.controller;

import com.burny.financas.auth.dto.AuthResponse;
import com.burny.financas.auth.dto.LoginHistoryResponse;
import com.burny.financas.auth.dto.LoginRequest;
import com.burny.financas.auth.dto.MessageResponse;
import com.burny.financas.auth.dto.RefreshTokenRequest;
import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.dto.UserResponse;
import com.burny.financas.auth.ratelimit.ClientIpResolver;
import com.burny.financas.auth.service.AuthService;
import com.burny.financas.auth.service.LoginHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginHistoryService loginHistoryService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, clientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request, clientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return new MessageResponse("Logged out successfully");
    }

    @GetMapping("/login-history")
    public List<LoginHistoryResponse> loginHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return loginHistoryService.getHistoryForUser(userId);
    }
}
