package com.hospitalmanagement.user.service;

import com.hospitalmanagement.user.entity.RefreshToken;
import com.hospitalmanagement.user.entity.User;

public interface RefreshTokenService {
    void validateRefreshToken(RefreshToken refreshToken);

    void revokeToken(RefreshToken refreshToken);

    String createAndSaveRefreshToken(User user);

    RefreshToken findRefreshTokenByRawToken(String refreshToken);

    String refreshToken(RefreshToken oldRefreshToken);
}
