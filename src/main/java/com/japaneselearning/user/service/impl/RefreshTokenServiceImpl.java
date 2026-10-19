package com.japaneselearning.user.service.impl;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.RefreshTokenRepository;
import com.japaneselearning.user.security.JwtProperties;
import com.japaneselearning.user.service.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtProperties jwtProperties;

    private final RefreshTokenRepository refreshTokenRepository;

    private static final int TOKEN_RANDOM_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    @Override
    @Transactional
    public String createAndSaveRefreshToken(User user) {
        return generateAndSaveRefreshToken(user);
    }

    @Override
    public RefreshToken findRefreshTokenByRawToken(String refreshToken) {
        String refreshTokenHash = hashRefreshToken(refreshToken);

        return refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND)
                );
    }

    @Override
    @Transactional
    public String refreshToken(RefreshToken oldRefreshToken) {
        validateRefreshToken(oldRefreshToken);

        revokeToken(oldRefreshToken);

        return generateAndSaveRefreshToken(oldRefreshToken.getUser());
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashRefreshToken(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to hash refresh token",
                    exception
            );
        }
    }

    private String generateAndSaveRefreshToken(User user) {
        String rawRefreshToken = generateRefreshToken();

        String refreshTokenHash = hashRefreshToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenHash,
                LocalDateTime.now().plusDays(jwtProperties.refreshTokenTtlDays())
        );

        refreshTokenRepository.save(refreshToken);

        return rawRefreshToken;
    }

    private void validateRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);
        }

        LocalDateTime now = LocalDateTime.now();

        if (!refreshToken.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }
    }

    private void revokeToken(RefreshToken refreshToken) {
        refreshToken.revoke();

        refreshTokenRepository.save(refreshToken);
    }
}
