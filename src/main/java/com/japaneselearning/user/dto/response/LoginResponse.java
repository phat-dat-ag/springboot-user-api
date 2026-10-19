package com.japaneselearning.user.dto.response;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
