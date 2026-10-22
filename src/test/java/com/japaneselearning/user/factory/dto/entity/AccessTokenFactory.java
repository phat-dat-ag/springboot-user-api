package com.japaneselearning.user.factory.dto.entity;

import com.japaneselearning.user.security.JwtTokenService;

public class AccessTokenFactory {
    public static JwtTokenService.GeneratedAccessToken createValid() {
        return new JwtTokenService.GeneratedAccessToken(
                "token",
                900
        );
    }
}
