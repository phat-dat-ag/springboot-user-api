package com.hospitalmanagement.user.dto.response;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
