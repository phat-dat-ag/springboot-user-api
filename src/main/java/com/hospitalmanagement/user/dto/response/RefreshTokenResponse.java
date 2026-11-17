package com.hospitalmanagement.user.dto.response;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
