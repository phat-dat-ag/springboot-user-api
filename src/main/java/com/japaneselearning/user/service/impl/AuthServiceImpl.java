package com.japaneselearning.user.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import com.japaneselearning.user.entity.User;
import com.japaneselearning.user.repository.UserRepository;
import com.japaneselearning.user.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RegisterAccountResponse registerAccount(RegisterAccountRequest registerAccountRequest) {

        String email = registerAccountRequest.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected: duplicate email");
            log.error("Email already exists");
        }

        String username = registerAccountRequest.username().trim();
        String passwordHash =
                passwordEncoder.encode(registerAccountRequest.password());

        User user = new User(username, email, passwordHash);

        User savedUser = userRepository.save(user);

        log.info("User registration completed, userId={}",
                savedUser.getId());

        return new RegisterAccountResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.isActivated()
        );
    }
}
