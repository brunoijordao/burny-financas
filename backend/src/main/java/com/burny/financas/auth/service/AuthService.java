package com.burny.financas.auth.service;

import com.burny.financas.auth.dto.AuthResponse;
import com.burny.financas.auth.dto.LoginRequest;
import com.burny.financas.auth.dto.RefreshTokenRequest;
import com.burny.financas.auth.dto.RegisterRequest;
import com.burny.financas.auth.dto.UserResponse;
import com.burny.financas.auth.entity.User;
import com.burny.financas.auth.event.UserRegisteredEvent;
import com.burny.financas.auth.exception.EmailAlreadyExistsException;
import com.burny.financas.auth.exception.InvalidCredentialsException;
import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.auth.security.JwtService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        user = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ip) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        boolean success = userOpt.isPresent()
                && passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash());

        // History is written for both outcomes and must never block/fail the login flow itself
        // (LoginHistoryService.record runs in its own transaction and swallows write failures).
        loginHistoryService.record(userOpt.orElse(null), request.email(), ip, success);

        if (!success) {
            // Same exception/message for "unknown email" and "wrong password" -> no user enumeration.
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userOpt.get();
        String accessToken = jwtService.generateAccessToken(user.getId());
        RefreshTokenService.IssuedToken issued = refreshTokenService.issue(user, ip);

        return new AuthResponse(accessToken, issued.plainValue(), "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ip) {
        RefreshTokenService.IssuedToken issued = refreshTokenService.rotate(request.refreshToken(), ip);
        String accessToken = jwtService.generateAccessToken(issued.entity().getUser().getId());
        return new AuthResponse(accessToken, issued.plainValue(), "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}
