package com.japaneselearning.user.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.japaneselearning.user.controller.AuthController;
import com.japaneselearning.user.dto.request.LoginRequest;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.LoginResponse;
import com.japaneselearning.user.repository.UserRepository;
import com.japaneselearning.user.repository.RefreshTokenRepository;
import com.japaneselearning.user.security.jwt.user.JwtProperties;
import com.japaneselearning.user.security.jwt.user.JwtTokenService;
import com.japaneselearning.user.service.AuthService;
import com.japaneselearning.user.service.RefreshTokenService;
import com.japaneselearning.user.service.impl.AuthServiceImpl;
import com.japaneselearning.user.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.util.*;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessLoggingPrivacyTest {
    @Test
    void authAndRefreshLogsExcludeCredentialsIdentifiersTokensAndHashesEvenAtDebug() throws Exception {
        var events = new ArrayList<ILoggingEvent>();
        var appender = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                events.add(event);
            }
        };
        appender.start();
        var levels = new HashMap<Logger, Level>();
        var additive = new HashMap<Logger, Boolean>();
        for (Class<?> type : List.of(AuthController.class, AuthServiceImpl.class, RefreshTokenServiceImpl.class)) {
            var logger = (Logger) LoggerFactory.getLogger(type);
            levels.put(logger, logger.getLevel());
            additive.put(logger, logger.isAdditive());
            logger.setLevel(Level.DEBUG);
            logger.setAdditive(false);
            logger.addAppender(appender);
        }
        var encoder = new StructuredJsonEncoder();
        encoder.setContext(((Logger) LoggerFactory.getLogger(AuthController.class)).getLoggerContext());
        encoder.start();
        try {
            String username = "private-username-sentinel", email = "private-email-sentinel@example.invalid";
            String password = "private-password-sentinel", token = "private-refresh-token-sentinel";
            var users = mock(UserRepository.class);
            var service = new AuthServiceImpl(users, mock(PasswordEncoder.class), mock(RefreshTokenService.class), mock(JwtTokenService.class));
            when(users.existsByEmail(email)).thenReturn(true);
            assertThatThrownBy(() -> service.registerAccount(new RegisterAccountRequest(username, email, password)));
            when(users.existsByEmail(email)).thenReturn(false);
            when(users.existsByUsername(username)).thenReturn(true);
            assertThatThrownBy(() -> service.registerAccount(new RegisterAccountRequest(username, email, password)));
            when(users.findByUsername(username)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.login(new LoginRequest(username, password)));
            var tokens = mock(RefreshTokenRepository.class);
            when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
            var refresh = new RefreshTokenServiceImpl(new JwtProperties("test", 900, 30), tokens);
            assertThatThrownBy(() -> refresh.findRefreshTokenByRawToken(token));
            var auth = mock(AuthService.class);
            when(auth.login(any())).thenReturn(new LoginResponse("Bearer", "private-access-token-sentinel", token, 900));
            new AuthController(auth).login(new LoginRequest(username, password));
            String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
            assertThat(events).isNotEmpty();
            StringBuilder rendered = new StringBuilder();
            for (var event : events) rendered.append(new String(encoder.encode(event), StandardCharsets.UTF_8));
            assertThat(rendered.toString()).doesNotContain(username, email, password, token, hash, "private-access-token-sentinel");
        } finally {
            encoder.stop();
            levels.forEach((logger, level) -> {
                logger.detachAppender(appender);
                logger.setLevel(level);
                logger.setAdditive(additive.get(logger));
            });
            appender.stop();
        }
    }
}
