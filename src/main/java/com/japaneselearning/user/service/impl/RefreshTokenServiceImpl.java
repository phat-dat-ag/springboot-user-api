package com.japaneselearning.user.service.impl;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.RefreshTokenRepository;
import com.japaneselearning.user.security.jwt.user.JwtProperties;
import com.japaneselearning.user.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtProperties jwtProperties;

    private final RefreshTokenRepository refreshTokenRepository;

    private static final int TOKEN_RANDOM_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void validateRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            log.debug("Refresh rejected: session already revoked");

            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);
        }

        if (refreshToken.isExpired()) {
            log.debug("Refresh rejected: session expired");

            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }
    }

    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.revoke();

        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh session marked revoked");
    }

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
                        () -> {
                            log.debug("Refresh rejected: token not found");

                            return new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND);
                        }
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

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        log.debug("Refresh session created");

        return rawRefreshToken;
    }
}
