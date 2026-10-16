package com.japaneselearning.user.controller;

import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @PostMapping
    public ResponseEntity<RegisterAccountResponse> Register(@Valid RegisterAccountRequest registerAccountRequest) {
        log.info("Register new account for username: {} ,email: {}", registerAccountRequest.username(), registerAccountRequest.email());

        return ResponseEntity.ok(new RegisterAccountResponse("refreshToken", "accessToken"));
    }
}
