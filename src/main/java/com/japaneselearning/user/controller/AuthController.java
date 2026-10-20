package com.japaneselearning.user.controller;

import com.japaneselearning.user.common.controller.BaseController;
import com.japaneselearning.user.common.response.APIResponse;
import com.japaneselearning.user.dto.request.LoginRequest;
import com.japaneselearning.user.dto.request.LogoutRequest;
import com.japaneselearning.user.dto.request.RefreshTokenRequest;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.LoginResponse;
import com.japaneselearning.user.dto.response.RefreshTokenResponse;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import com.japaneselearning.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Slf4j
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<@NonNull APIResponse<RegisterAccountResponse>> registerAccount(
            @Valid @RequestBody RegisterAccountRequest registerAccountRequest
    ) {
        log.info(
                "Received account registration request: username={}, email={}",
                registerAccountRequest.username(),
                registerAccountRequest.email()
        );

        RegisterAccountResponse response = authService.registerAccount(registerAccountRequest);

        log.info(
                "Account registration completed successfully: username={}",
                registerAccountRequest.username()
        );

        return success(response);
    }


    @PostMapping("/login")
    public ResponseEntity<@NonNull APIResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        log.info("Received login request: identifier={}", loginRequest.usernameOrEmail());

        LoginResponse response = authService.login(loginRequest);

        log.info("Login completed successfully");

        return success(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<@NonNull APIResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        log.info("Received refresh token request");

        RefreshTokenResponse response = authService.refreshToken(refreshTokenRequest);

        log.info("Refresh token completed successfully");

        return success(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<@NonNull APIResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest logoutRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Received logout request: userId={}", jwt.getSubject());

        authService.logout(logoutRequest.refreshToken());

        log.info("Logout completed successfully");

        return successWithoutContent();
    }
}
