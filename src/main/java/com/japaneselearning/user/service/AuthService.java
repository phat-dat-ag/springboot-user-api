package com.japaneselearning.user.service;

import com.japaneselearning.user.dto.request.LoginRequest;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.request.RefreshTokenRequest;
import com.japaneselearning.user.dto.response.LoginResponse;
import com.japaneselearning.user.dto.response.RefreshTokenResponse;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;

public interface AuthService {
    RegisterAccountResponse registerAccount(RegisterAccountRequest registerAccountRequest);

    LoginResponse login(LoginRequest loginRequest);

    RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(String refreshToken);
}
