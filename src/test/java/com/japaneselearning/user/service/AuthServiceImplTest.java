package com.japaneselearning.user.service;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.dto.request.LoginRequest;
import com.japaneselearning.user.dto.request.RefreshTokenRequest;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.LoginResponse;
import com.japaneselearning.user.dto.response.RefreshTokenResponse;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.factory.dto.entity.AccessTokenFactory;
import com.japaneselearning.user.factory.dto.entity.RefreshTokenFactory;
import com.japaneselearning.user.factory.dto.entity.UserFactory;
import com.japaneselearning.user.factory.dto.request.LoginRequestFactory;
import com.japaneselearning.user.factory.dto.request.RefreshTokenRequestFactory;
import com.japaneselearning.user.factory.dto.request.RegisterAccountRequestFactory;
import com.japaneselearning.user.repository.UserRepository;
import com.japaneselearning.user.security.JwtTokenService;
import com.japaneselearning.user.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.Mock;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private static final String PASSWORD_HASH = "password_hash";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String RAW_REFRESH_TOKEN = "raw_refresh_token";

    @Test
    void registerAccount_shouldSuccess() {
        RegisterAccountRequest validRequest = RegisterAccountRequestFactory.createValid();

        when(userRepository.existsByEmail(validRequest.email())).thenReturn(false);

        when(userRepository.existsByUsername(validRequest.username())).thenReturn(false);

        when(passwordEncoder.encode(validRequest.password())).thenReturn(PASSWORD_HASH);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterAccountResponse response = authService.registerAccount(validRequest);

        assertNotNull(response);

        assertEquals(validRequest.username(), response.username());
        assertEquals(validRequest.email(), response.email());

        assertTrue(response.isActivated());
    }

    @Test
    void registerAccount_shouldFailed_whenEmailExisting() {
        RegisterAccountRequest validRequest = RegisterAccountRequestFactory.createValid();

        when(userRepository.existsByEmail(validRequest.email())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.registerAccount(validRequest)
        );

        assertEquals(
                ErrorCode.USER_EMAIL_ALREADY_EXISTS,
                exception.getErrorCode()
        );
    }

    @Test
    void registerAccount_shouldFailed_whenUsernameExisting() {
        RegisterAccountRequest validRequest = RegisterAccountRequestFactory.createValid();

        when(userRepository.existsByEmail(validRequest.email())).thenReturn(false);

        when(userRepository.existsByUsername(validRequest.username())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.registerAccount(validRequest)
        );

        assertEquals(
                ErrorCode.USER_USERNAME_ALREADY_EXISTS,
                exception.getErrorCode()
        );
    }

    @Test
    void login_shouldSuccess_usingEmail() {
        User validUser = UserFactory.createValid();

        LoginRequest validRequestWithEmail = LoginRequestFactory.createValidWithEmail();

//        Arrange

        when(userRepository.findByEmail(validRequestWithEmail.usernameOrEmail()))
                .thenReturn(Optional.of(validUser));

        when(passwordEncoder.matches(
                validRequestWithEmail.password(),
                validUser.getPasswordHash())
        ).thenReturn(true);

        JwtTokenService.GeneratedAccessToken accessToken = AccessTokenFactory.createValid();

        when(jwtTokenService.generateAccessToken(validUser))
                .thenReturn(accessToken);

        when(refreshTokenService.createAndSaveRefreshToken(validUser))
                .thenReturn(RAW_REFRESH_TOKEN);

//        Act

        LoginResponse response = authService.login(validRequestWithEmail);

//        Assert

        assertEquals(accessToken.token(), response.accessToken());
        assertEquals(RAW_REFRESH_TOKEN, response.refreshToken());

//        Verify

        verify(userRepository).findByEmail(validRequestWithEmail.usernameOrEmail());

        verify(passwordEncoder).matches(
                validRequestWithEmail.password(),
                validUser.getPasswordHash()
        );

        verify(jwtTokenService).generateAccessToken(validUser);

        verify(refreshTokenService).createAndSaveRefreshToken(validUser);
    }

    @Test
    void login_shouldSuccess_usingUsername() {
        User validUser = UserFactory.createValid();

        LoginRequest validRequestWithEmail = LoginRequestFactory.createValidWithUsername();

//        Arrange

        when(userRepository.findByUsername(validRequestWithEmail.usernameOrEmail()))
                .thenReturn(Optional.of(validUser));

        when(passwordEncoder.matches(
                validRequestWithEmail.password(),
                validUser.getPasswordHash())
        ).thenReturn(true);

        JwtTokenService.GeneratedAccessToken accessToken = AccessTokenFactory.createValid();

        when(jwtTokenService.generateAccessToken(validUser))
                .thenReturn(accessToken);

        when(refreshTokenService.createAndSaveRefreshToken(validUser))
                .thenReturn(RAW_REFRESH_TOKEN);

//        Act

        LoginResponse response = authService.login(validRequestWithEmail);

//        Assert

        assertEquals(accessToken.token(), response.accessToken());
        assertEquals(RAW_REFRESH_TOKEN, response.refreshToken());

//        Verify

        verify(userRepository).findByUsername(validRequestWithEmail.usernameOrEmail());

        verify(passwordEncoder).matches(
                validRequestWithEmail.password(),
                validUser.getPasswordHash()
        );

        verify(jwtTokenService).generateAccessToken(validUser);

        verify(refreshTokenService).createAndSaveRefreshToken(validUser);
    }

    @Test
    void login_shouldFailed_usingEmail_whenUserNotFound() {
        LoginRequest validRequestWithEmail = LoginRequestFactory.createValidWithEmail();

//        Arrange

        when(userRepository.findByEmail(validRequestWithEmail.usernameOrEmail()))
                .thenReturn(Optional.empty());

//        Act

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(validRequestWithEmail)
        );

//        Assert

        assertEquals(ErrorCode.USER_INVALID_CREDENTIALS, exception.getErrorCode());

//        Verify

        verify(userRepository).findByEmail(validRequestWithEmail.usernameOrEmail());

        verify(passwordEncoder, never()).matches(anyString(), anyString());

        verify(jwtTokenService, never()).generateAccessToken(any(User.class));

        verify(refreshTokenService, never()).createAndSaveRefreshToken(any(User.class));
    }

    @Test
    void login_shouldFailed_usingEmail_whenPasswordNotMatched() {
        User validUser = UserFactory.createValid();

        LoginRequest validRequestWithEmail = LoginRequestFactory.createValidWithEmail();

//        Arrange

        when(userRepository.findByEmail(validRequestWithEmail.usernameOrEmail()))
                .thenReturn(Optional.of(validUser));

        when(passwordEncoder.matches(
                validRequestWithEmail.password(),
                validUser.getPasswordHash())
        ).thenReturn(false);

//        Act

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(validRequestWithEmail)
        );

//        Assert

        assertEquals(ErrorCode.USER_INVALID_CREDENTIALS, exception.getErrorCode());

//        Verify

        verify(userRepository).findByEmail(validRequestWithEmail.usernameOrEmail());

        verify(passwordEncoder).matches(
                validRequestWithEmail.password(),
                validUser.getPasswordHash()
        );

        verify(jwtTokenService, never()).generateAccessToken(any(User.class));

        verify(refreshTokenService, never()).createAndSaveRefreshToken(any(User.class));
    }

    @Test
    void login_shouldFailed_usingEmail_whenAccountNotActivated() {
        User deactivatedUser = UserFactory.createNotActivated();

        LoginRequest validRequestWithEmail = LoginRequestFactory.createValidWithEmail();

//        Arrange

        when(userRepository.findByEmail(validRequestWithEmail.usernameOrEmail()))
                .thenReturn(Optional.of(deactivatedUser));

        when(passwordEncoder.matches(
                validRequestWithEmail.password(),
                deactivatedUser.getPasswordHash())
        ).thenReturn(true);

//        Act

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(validRequestWithEmail)
        );

//        Assert

        assertEquals(ErrorCode.USER_ACCOUNT_NOT_ACTIVATED, exception.getErrorCode());

//        Verify

        verify(userRepository).findByEmail(validRequestWithEmail.usernameOrEmail());

        verify(passwordEncoder).matches(
                validRequestWithEmail.password(),
                deactivatedUser.getPasswordHash()
        );

        verify(jwtTokenService, never()).generateAccessToken(any(User.class));

        verify(refreshTokenService, never()).createAndSaveRefreshToken(any(User.class));
    }

    @Test
    void refreshToken_shouldSuccess() {
//        Arrange

        RefreshTokenRequest validRequest = RefreshTokenRequestFactory.createValid();

        RefreshToken oldRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(validRequest.refreshToken()))
                .thenReturn(oldRefreshToken);

        when(refreshTokenService.refreshToken(oldRefreshToken))
                .thenReturn(RAW_REFRESH_TOKEN);

        JwtTokenService.GeneratedAccessToken accessToken = AccessTokenFactory.createValid();

        when(jwtTokenService.generateAccessToken(oldRefreshToken.getUser()))
                .thenReturn(accessToken);

//        Act

        RefreshTokenResponse response = authService.refreshToken(validRequest);

//        Assert

        assertEquals(accessToken.token(), response.accessToken());
        assertEquals(RAW_REFRESH_TOKEN, response.refreshToken());
        assertEquals(accessToken.expiresIn(), response.expiresIn());

//        Verify

        verify(refreshTokenService).findRefreshTokenByRawToken(validRequest.refreshToken());
        verify(refreshTokenService).refreshToken(oldRefreshToken);
        verify(jwtTokenService).generateAccessToken(oldRefreshToken.getUser());
    }

    @Test
    void refreshToken_shouldFailed_whenRefreshTokenNotFound() {
//        Arrange

        RefreshTokenRequest validRequest = RefreshTokenRequestFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(validRequest.refreshToken()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND));

//        Act

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshToken(validRequest)
        );

//        Assert

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());

//        Verify

        verify(refreshTokenService).findRefreshTokenByRawToken(validRequest.refreshToken());
    }

    @Test
    void refreshToken_shouldFailed_whenTokenRevoked() {
//        Arrange

        RefreshTokenRequest validRequest = RefreshTokenRequestFactory.createValid();

        RefreshToken oldRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(validRequest.refreshToken()))
                .thenReturn(oldRefreshToken);

        when(refreshTokenService.refreshToken(oldRefreshToken))
                .thenThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED));

//        Act

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshToken(validRequest)
        );

//        Assert

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, exception.getErrorCode());

//        Verify

        verify(refreshTokenService).findRefreshTokenByRawToken(validRequest.refreshToken());
        verify(refreshTokenService).refreshToken(oldRefreshToken);
    }

    @Test
    void refreshToken_shouldFailed_whenTokenExpired() {
//        Arrange

        RefreshTokenRequest validRequest = RefreshTokenRequestFactory.createValid();

        RefreshToken oldRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(validRequest.refreshToken()))
                .thenReturn(oldRefreshToken);

        when(refreshTokenService.refreshToken(oldRefreshToken))
                .thenThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED));

//        Act

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshToken(validRequest)
        );

//        Assert

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED, exception.getErrorCode());

//        Verify

        verify(refreshTokenService).findRefreshTokenByRawToken(validRequest.refreshToken());
        verify(refreshTokenService).refreshToken(oldRefreshToken);
    }

    @Test
    void logout_shouldSuccess() {
        RefreshToken oldRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(REFRESH_TOKEN))
                .thenReturn(oldRefreshToken);

        authService.logout(REFRESH_TOKEN);

        verify(refreshTokenService).validateRefreshToken(oldRefreshToken);
        verify(refreshTokenService).revokeToken(oldRefreshToken);
    }

    @Test
    void logout_shouldFailed_whenRefreshTokenNotFound() {
        when(refreshTokenService.findRefreshTokenByRawToken(REFRESH_TOKEN))
                .thenThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.logout(REFRESH_TOKEN)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());

        verify(refreshTokenService, never()).revokeToken(any(RefreshToken.class));
    }

    @Test
    void logout_shouldFailed_whenRefreshTokenRevoked() {
        RefreshToken revokedRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(REFRESH_TOKEN))
                .thenReturn(revokedRefreshToken);

        doThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED))
                .when(refreshTokenService)
                .validateRefreshToken(revokedRefreshToken);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.logout(REFRESH_TOKEN)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, exception.getErrorCode());

        verify(refreshTokenService).validateRefreshToken(revokedRefreshToken);
        verify(refreshTokenService, never()).revokeToken(any(RefreshToken.class));
    }

    @Test
    void logout_shouldFailed_whenRefreshTokenExpired() {
        RefreshToken expiredRefreshToken = RefreshTokenFactory.createValid();

        when(refreshTokenService.findRefreshTokenByRawToken(REFRESH_TOKEN))
                .thenReturn(expiredRefreshToken);

        doThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED))
                .when(refreshTokenService)
                .validateRefreshToken(expiredRefreshToken);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.logout(REFRESH_TOKEN)
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED, exception.getErrorCode());

        verify(refreshTokenService).validateRefreshToken(expiredRefreshToken);
        verify(refreshTokenService, never()).revokeToken(any(RefreshToken.class));
    }
}
