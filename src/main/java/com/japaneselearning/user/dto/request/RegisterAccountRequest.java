package com.japaneselearning.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 100, message = "username should have from 3 to 100 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        @Size(max = 100, message = "email length should be less or equal to 100")
        String email,

        @NotBlank(message = "password id required")
        @Size(min = 8, max = 50, message = "password should have from 8 to 50 characters")
        String password
) {
}
