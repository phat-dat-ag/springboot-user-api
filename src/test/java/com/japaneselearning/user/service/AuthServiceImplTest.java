package com.japaneselearning.user.service;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.factory.dto.request.RegisterAccountRequestFactory;
import com.japaneselearning.user.repository.UserRepository;
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
import static org.mockito.Mockito.when;

import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD_HASH = "password_hash";

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
}
