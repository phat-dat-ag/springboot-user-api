package com.japaneselearning.user.service;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.factory.dto.entity.RefreshTokenFactory;
import com.japaneselearning.user.repository.RefreshTokenRepository;
import com.japaneselearning.user.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceImplTest {

    @InjectMocks
    RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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
}
