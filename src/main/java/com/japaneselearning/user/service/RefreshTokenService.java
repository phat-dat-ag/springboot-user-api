package com.japaneselearning.user.service;

import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;

public interface RefreshTokenService {
    void validateRefreshToken(RefreshToken refreshToken);

    void revokeToken(RefreshToken refreshToken);

    String createAndSaveRefreshToken(User user);

    RefreshToken findRefreshTokenByRawToken(String refreshToken);

    String refreshToken(RefreshToken oldRefreshToken);
}
