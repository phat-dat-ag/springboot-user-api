package com.japaneselearning.user.factory.dto.entity;

import com.japaneselearning.user.entity.RefreshToken;

import java.time.LocalDateTime;

public class RefreshTokenFactory {

    private static final String TOKEN_HASH = "token_hash";

    private static final LocalDateTime ONE_MINUTE_AGO = LocalDateTime.now().minusMinutes(1);
    private static final LocalDateTime ONE_MINUTE_LATER = LocalDateTime.now().plusMinutes(1);

    public static RefreshToken createValid() {
        return new RefreshToken(
                UserFactory.createValid(),
                TOKEN_HASH,
                ONE_MINUTE_LATER
        );
    }

    public static RefreshToken createRevoked() {
        RefreshToken refreshToken = new RefreshToken(
                UserFactory.createValid(),
                TOKEN_HASH,
                ONE_MINUTE_LATER
        );

        refreshToken.setRevokedAt(ONE_MINUTE_AGO);

        return refreshToken;
    }

    public static RefreshToken createExpired() {
        return new RefreshToken(
                UserFactory.createValid(),
                TOKEN_HASH,
                ONE_MINUTE_AGO
        );
    }
}
