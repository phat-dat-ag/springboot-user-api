package com.hospitalmanagement.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserActivationRequest(
        @NotNull(message = "isActivated is required")
        Boolean isActivated
) {
}
