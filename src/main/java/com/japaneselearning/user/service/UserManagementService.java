package com.japaneselearning.user.service;

import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.response.PageResponse;

public interface UserManagementService {
    PageResponse<UserDTO> getAllUsers(
            int page,
            int size,
            boolean isActivated,
            String usernamePattern,
            String emailPattern
    );
}
