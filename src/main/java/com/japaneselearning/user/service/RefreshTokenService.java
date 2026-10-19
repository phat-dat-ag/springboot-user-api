package com.japaneselearning.user.service;

import com.japaneselearning.user.entity.User;

public interface RefreshTokenService {
    String createAndSaveRefreshToken(User user);
}
