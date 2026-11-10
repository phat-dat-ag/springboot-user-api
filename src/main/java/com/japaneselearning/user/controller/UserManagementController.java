package com.japaneselearning.user.controller;

import com.japaneselearning.user.common.controller.BaseController;
import com.japaneselearning.user.common.response.APIResponse;
import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.request.UpdateUserActivationRequest;
import com.japaneselearning.user.dto.response.PageResponse;
import com.japaneselearning.user.dto.response.UserResponse;
import com.japaneselearning.user.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController extends BaseController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<@NonNull APIResponse<PageResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be greater than 0")
            @Max(value = 100, message = "Size must not exceed 100")
            int size,

            @RequestParam(defaultValue = "false")
            boolean isActivated,

            @RequestParam(required = false)
            String usernamePattern,

            @RequestParam(required = false)
            String emailPattern
    ) {
        return success(userManagementService.getAllUsers(
                page,
                size,
                isActivated,
                usernamePattern,
                emailPattern
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull APIResponse<UserResponse>> getUser(
            @PathVariable
            UUID id
    ) {
        log.debug("User lookup requested");

        UserResponse response = userManagementService.getUser(id);

        log.debug("User lookup completed");

        return success(response);
    }

    @PatchMapping("{id}/activation")
    public ResponseEntity<@NonNull APIResponse<Void>> updateUserActivation(
            @PathVariable
            UUID id,

            @Valid
            @RequestBody
            UpdateUserActivationRequest updateUserActivationRequest
    ) {
        log.debug("User activation update requested");

        userManagementService.updateUserActivation(id, updateUserActivationRequest.isActivated());

        log.debug("User activation update completed");

        return successWithoutContent();
    }
}
