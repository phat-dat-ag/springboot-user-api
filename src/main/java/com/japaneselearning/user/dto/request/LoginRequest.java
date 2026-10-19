package com.japaneselearning.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "usernameOrEmail is required")
        @Size(min = 3, max = 100, message = "usernameOrEmail should have from 3 to 100 characters")
        String usernameOrEmail,

        @NotBlank(message = "password id required")
        @Size(min = 8, max = 50, message = "password should have from 8 to 50 characters")
        String password
) {
}
