package com.japaneselearning.user.service;

import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.response.PageResponse;
import com.japaneselearning.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserManagementService {
    PageResponse<UserDTO> getAllUsers(
            int page,
            int size,
            boolean isActivated,
            String usernamePattern,
            String emailPattern
    );

    UserResponse getUser(UUID uuid);

    void updateUserActivation(UUID uuid, boolean newActivationStatus);
}
