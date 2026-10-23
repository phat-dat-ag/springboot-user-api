package com.japaneselearning.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String email,
        boolean isActivated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
