package com.japaneselearning.user.controller;

import com.japaneselearning.user.common.controller.BaseController;
import com.japaneselearning.user.common.response.APIResponse;
import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;
import com.japaneselearning.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Slf4j
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<APIResponse<RegisterAccountResponse>> registerAccount(
            @Valid RegisterAccountRequest registerAccountRequest
    ) {
        log.info(
                "Register new account for username: {} ,email: {}",
                registerAccountRequest.username(),
                registerAccountRequest.email()
        );

        RegisterAccountResponse response = authService.registerAccount(registerAccountRequest);

        return success(response);
    }
}
