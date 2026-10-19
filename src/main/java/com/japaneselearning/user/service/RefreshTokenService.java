package com.japaneselearning.user.service;

import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;

public interface RefreshTokenService {
    String createAndSaveRefreshToken(User user);

    RefreshToken findRefreshTokenByRawToken(String refreshToken);

    String refreshToken(RefreshToken oldRefreshToken);
}
