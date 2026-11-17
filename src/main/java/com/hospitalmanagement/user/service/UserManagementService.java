package com.hospitalmanagement.user.service;

import com.hospitalmanagement.user.dto.UserDTO;
import com.hospitalmanagement.user.dto.response.PageResponse;
import com.hospitalmanagement.user.dto.response.UserResponse;

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
