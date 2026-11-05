package com.japaneselearning.user.controller.internal;

import com.japaneselearning.user.common.exception.BusinessException;
import com.japaneselearning.user.common.exception.ErrorCode;
import com.japaneselearning.user.config.SecurityConfig;
import com.japaneselearning.user.controller.UserManagementController;
import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.response.UserResponse;
import com.japaneselearning.user.security.JwtRoleConverter;
import com.japaneselearning.user.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({InternalUserController.class, UserManagementController.class})
@Import({SecurityConfig.class, JwtRoleConverter.class})
class InternalUserControllerTest {
    private static final UUID ID = UUID.fromString("f05bf709-5cb8-4a4c-9ac4-e537b5579aef");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserManagementService users;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private UserResponse user() {
        return new UserResponse(new UserDTO(ID, "test-user", "test@example.invalid", true,
                LocalDateTime.of(2026, 11, 1, 10, 0), null));
    }

    @Test
    void internalEndpointRemainsUnauthenticatedAndReturnsUnwrappedUser() throws Exception {
        given(users.getUser(ID)).willReturn(user());
        mvc.perform(get("/internal/users/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(ID.toString()))
                .andExpect(jsonPath("$.user.username").value("test-user"))
                .andExpect(jsonPath("$.user.email").value("test@example.invalid"))
                .andExpect(jsonPath("$.user.isActivated").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void missingUserRetains404Envelope() throws Exception {
        given(users.getUser(ID)).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));
        mvc.perform(get("/internal/users/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void malformedIdDoesNotCallService() throws Exception {
        // Preserve the shared exception handler's current response; changing it affects external APIs too.
        mvc.perform(get("/internal/users/not-a-uuid")).andExpect(status().isInternalServerError());
        verifyNoInteractions(users);
    }

    @Test
    void externalEndpointStillRequiresAuthenticationAndAdminRole() throws Exception {
        mvc.perform(get("/admin/users/" + ID)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/users/" + ID).with(jwt().authorities(createAuthorityList("ROLE_USER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    @Test
    void externalAdminEndpointRetainsResponseEnvelope() throws Exception {
        given(users.getUser(ID)).willReturn(user());
        mvc.perform(get("/admin/users/" + ID).with(jwt().authorities(createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.id").value(ID.toString()));
    }

    @Test
    void otherInternalPathsRemainProtected() throws Exception {
        mvc.perform(get("/internal/other")).andExpect(status().isUnauthorized());
    }
}
