package com.hospitalmanagement.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record InternalUserResponse(Account user) {
    public record Account(
            UUID id,
            String username,
            String email,
            boolean isActivated,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            short role
    ) {
    }
}
