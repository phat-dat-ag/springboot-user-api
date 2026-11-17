package com.hospitalmanagement.user.controller.internal;

import com.hospitalmanagement.user.dto.response.UserResponse;
import com.hospitalmanagement.user.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service user lookup. InternalApiSecurityConfig authenticates Hospital before
 * dispatching here. Success returns UserResponse directly, without APIResponse.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserManagementService userManagementService;

    @GetMapping("/{id}")
    public UserResponse getUser(
            @PathVariable UUID id
    ) {
        return userManagementService.getUser(id);
    }
}
