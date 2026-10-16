package com.japaneselearning.user.dto.response;

import java.util.UUID;

public record RegisterAccountResponse(
        UUID id,
        String username,
        String email,
        boolean isActivated
) {
}
