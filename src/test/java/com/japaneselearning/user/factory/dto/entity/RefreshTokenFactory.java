package com.japaneselearning.user.factory.dto.entity;

import com.japaneselearning.user.entity.RefreshToken;

import java.time.LocalDateTime;

public class RefreshTokenFactory {

    private static final String TOKEN_HASH = "token_hash";

    public static RefreshToken createValid() {
        return new RefreshToken(
                UserFactory.createValid(),
                TOKEN_HASH,
                LocalDateTime.now()
        );
    }
}
