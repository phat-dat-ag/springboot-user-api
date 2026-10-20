package com.japaneselearning.user.service.impl;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.dto.request.LoginRequest;
import com.japaneselearning.user.dto.request.RefreshTokenRequest;
import com.japaneselearning.user.dto.response.LoginResponse;
import com.japaneselearning.user.dto.response.RefreshTokenResponse;
import com.japaneselearning.user.entity.RefreshToken;
import com.japaneselearning.user.security.JwtTokenService;
import com.japaneselearning.user.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.UserRepository;
import com.japaneselearning.user.service.AuthService;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional
    public RegisterAccountResponse registerAccount(RegisterAccountRequest registerAccountRequest) {

        String email = registerAccountRequest.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        String username = registerAccountRequest.username().trim();

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USER_USERNAME_ALREADY_EXISTS);
        }

        String passwordHash =
                passwordEncoder.encode(registerAccountRequest.password());

        User user = new User(username, email, passwordHash);

        User savedUser = userRepository.save(user);

        log.info(
                "User registration completed, userId={}",
                savedUser.getId()
        );

        return new RegisterAccountResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.isActivated()
        );
    }

    private User retrieveActivatedAccount(String identifier, String password) {

        User user;

        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier.toLowerCase(Locale.ROOT)).orElse(null);
        } else {
            user = userRepository.findByUsername(identifier).orElse(null);
        }

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USER_INVALID_CREDENTIALS);
        }

        if (!user.isActivated()) {
            throw new BusinessException(ErrorCode.USER_ACCOUNT_NOT_ACTIVATED);
        }

        return user;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {

        String identifier = loginRequest.usernameOrEmail().trim();

        User user = retrieveActivatedAccount(identifier, loginRequest.password());

        JwtTokenService.GeneratedAccessToken accessToken = jwtTokenService.generateAccessToken(user);

        String rawRefreshToken = refreshTokenService.createAndSaveRefreshToken(user);

        log.info("User login successful, userId={}", user.getId());

        return new LoginResponse(
                "Bearer",
                accessToken.token(),
                rawRefreshToken,
                accessToken.expiresIn()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        RefreshToken oldRefreshToken =
                refreshTokenService.findRefreshTokenByRawToken(refreshTokenRequest.refreshToken());

        String newRawRefreshToken = refreshTokenService.refreshToken(oldRefreshToken);

        JwtTokenService.GeneratedAccessToken accessToken
                = jwtTokenService.generateAccessToken(oldRefreshToken.getUser());

        return new RefreshTokenResponse(
                accessToken.token(),
                newRawRefreshToken,
                accessToken.expiresIn()
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken oldRefreshToken = refreshTokenService.findRefreshTokenByRawToken(refreshToken);

        refreshTokenService.validateRefreshToken(oldRefreshToken);

        refreshTokenService.revokeToken(oldRefreshToken);
    }
}
