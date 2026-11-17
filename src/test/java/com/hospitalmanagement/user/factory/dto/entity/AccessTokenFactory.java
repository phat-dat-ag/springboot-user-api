package com.hospitalmanagement.user.factory.dto.entity;

import com.hospitalmanagement.user.security.jwt.user.JwtTokenService;

public class AccessTokenFactory {
    public static JwtTokenService.GeneratedAccessToken createValid() {
        return new JwtTokenService.GeneratedAccessToken(
                "token",
                900
        );
    }
}
