package com.hospitalmanagement.user.service;

import com.hospitalmanagement.user.dto.request.LoginRequest;
import com.hospitalmanagement.user.dto.request.RegisterAccountRequest;
import com.hospitalmanagement.user.dto.request.RefreshTokenRequest;
import com.hospitalmanagement.user.dto.response.LoginResponse;
import com.hospitalmanagement.user.dto.response.RefreshTokenResponse;
import com.hospitalmanagement.user.dto.response.RegisterAccountResponse;

public interface AuthService {
    RegisterAccountResponse registerAccount(RegisterAccountRequest registerAccountRequest);

    LoginResponse login(LoginRequest loginRequest);

    RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(String refreshToken);
}
