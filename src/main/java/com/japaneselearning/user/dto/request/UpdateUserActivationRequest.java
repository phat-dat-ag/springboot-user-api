package com.japaneselearning.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserActivationRequest(
        @NotNull(message = "isActivated is required")
        Boolean isActivated
) {
}
