package com.japaneselearning.user.security.jwt.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        long accessTokenTtlSeconds,
        long refreshTokenTtlDays
) {
}