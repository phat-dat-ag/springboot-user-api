package com.japaneselearning.user.service.impl;

import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.RefreshTokenRepository;
import com.japaneselearning.user.security.JwtProperties;
import com.japaneselearning.user.service.RefreshTokenService;
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
    public String createAndSaveRefreshToken(User user) {
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
}
