package com.japaneselearning.user.service;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.factory.dto.entity.RefreshTokenFactory;
import com.japaneselearning.user.factory.dto.entity.UserFactory;
import com.japaneselearning.user.repository.RefreshTokenRepository;
import com.japaneselearning.user.security.JwtProperties;
import com.japaneselearning.user.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceImplTest {

    @InjectMocks
    RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            "japanese-learning",
            900L,
            7L
    );

    private static final String RAW_TOKEN = "raw_token";

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(
                JWT_PROPERTIES,
                refreshTokenRepository
        );
    }

    @Test
    void validateRefreshToken_shouldSuccess() {
        RefreshToken refreshToken = RefreshTokenFactory.createValid();

        refreshTokenService.validateRefreshToken(refreshToken);

        assertFalse(refreshToken.isRevoked());
        assertFalse(refreshToken.isExpired());
    }

    @Test
    void validateRefreshToken_shouldFailed_whenRefreshTokenRevoked() {
        RefreshToken revokedRefreshToken = RefreshTokenFactory.createRevoked();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.validateRefreshToken(revokedRefreshToken)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, exception.getErrorCode());
    }

    @Test
    void validateRefreshToken_shouldFailed_whenRefreshTokenExpired() {
        RefreshToken expiredRefreshToken = RefreshTokenFactory.createExpired();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.validateRefreshToken(expiredRefreshToken)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void revokeToken_shouldSuccess() {
        RefreshToken refreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenRepository.save(refreshToken)).thenReturn(any(RefreshToken.class));

        refreshTokenService.revokeToken(refreshToken);

        assertTrue(refreshToken.isRevoked());

        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void createAndSaveRefreshToken_shouldSuccess() {
        User user = UserFactory.createValid();

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawRefreshToken = refreshTokenService.createAndSaveRefreshToken(user);

        assertNotNull(rawRefreshToken);
        assertFalse(rawRefreshToken.isBlank());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void findRefreshTokenByRawToken_shouldSuccess() {
        RefreshToken refreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(refreshToken));


        RefreshToken foundRefreshToken = refreshTokenService.findRefreshTokenByRawToken(RAW_TOKEN);

        assertNotNull(foundRefreshToken);

        verify(refreshTokenRepository).findByTokenHash(anyString());
    }

    @Test
    void findRefreshTokenByRawToken_shouldFailed_whenRefreshTokenNotFound() {
        RefreshToken refreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());


        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.findRefreshTokenByRawToken(RAW_TOKEN)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());

        verify(refreshTokenRepository).findByTokenHash(anyString());
    }

    @Test
    void refreshToken_shouldSuccess() {
        RefreshToken oldRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawRefreshToken = refreshTokenService.refreshToken(oldRefreshToken);

        assertNotNull(rawRefreshToken);

        assertFalse(rawRefreshToken.isBlank());

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_shouldFailed_whenRefreshTokenRevoked() {
        RefreshToken oldRefreshToken = RefreshTokenFactory.createRevoked();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.refreshToken(oldRefreshToken)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, exception.getErrorCode());

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_shouldFailed_whenRefreshTokenExpired() {
        RefreshToken oldRefreshToken = RefreshTokenFactory.createExpired();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.refreshToken(oldRefreshToken)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED, exception.getErrorCode());

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
