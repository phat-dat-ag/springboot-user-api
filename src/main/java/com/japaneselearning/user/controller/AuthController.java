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
        log.debug("Account registration requested");

        RegisterAccountResponse response = authService.registerAccount(registerAccountRequest);

        return success(response);
    }


    @PostMapping("/login")
    public ResponseEntity<@NonNull APIResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        log.debug("Login requested");

        LoginResponse response = authService.login(loginRequest);

        return success(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<@NonNull APIResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        log.debug("Token refresh requested");

        RefreshTokenResponse response = authService.refreshToken(refreshTokenRequest);

        return success(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<@NonNull APIResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest logoutRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.debug("Logout requested");

        authService.logout(logoutRequest.refreshToken());

        return successWithoutContent();
    }
}
