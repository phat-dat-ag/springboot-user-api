package com.hospitalmanagement.user.controller.internal;

import com.hospitalmanagement.user.entity.User;
import com.hospitalmanagement.user.repository.UserRepository;
import com.hospitalmanagement.user.security.config.InternalApiSecurityConfig;
import com.hospitalmanagement.user.security.config.UserSecurityConfig;
import com.hospitalmanagement.user.controller.UserManagementController;
import com.hospitalmanagement.user.security.jwt.user.JwtRoleConverter;
import com.hospitalmanagement.user.service.impl.UserManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({InternalUserController.class, UserManagementController.class})
@Import({
        InternalApiSecurityConfig.class,
        UserSecurityConfig.class,
        JwtRoleConverter.class,
        UserManagementServiceImpl.class
})
class InternalUserControllerTest {
    private static final UUID ID = UUID.fromString("f05bf709-5cb8-4a4c-9ac4-e537b5579aef");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserRepository users;

    @MockitoBean(name = "jwtDecoder")
    JwtDecoder jwtDecoder;

    @MockitoBean(name = "internalJwtDecoder")
    JwtDecoder internalJwtDecoder;

    private User user() {
        var user = new User("test-user", "test@example.invalid", "synthetic-password-hash");
        user.setId(ID);
        user.setCreatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 11, 2, 10, 0));
        user.setRole((short) 4);
        return user;
    }

    @Test
    void authenticatedServiceReturnsUnwrappedUser() throws Exception {
        given(users.findById(ID)).willReturn(Optional.of(user()));
        mvc.perform(get("/internal/users/" + ID).with(jwt().authorities(createAuthorityList("SERVICE_HOSPITAL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(ID.toString()))
                .andExpect(jsonPath("$.user.username").value("test-user"))
                .andExpect(jsonPath("$.user.email").value("test@example.invalid"))
                .andExpect(jsonPath("$.user.isActivated").value(true))
                .andExpect(jsonPath("$.user.createdAt").value("2026-11-01T10:00:00"))
                .andExpect(jsonPath("$.user.updatedAt").value("2026-11-02T10:00:00"))
                .andExpect(jsonPath("$.user.role").value(4))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void reflectsCurrentActivationAndRoleFromRepository() throws Exception {
        User account = user();
        account.setActivated(false);
        given(users.findById(ID)).willReturn(Optional.of(account));

        var request = get("/internal/users/" + ID)
                .with(jwt().authorities(createAuthorityList("SERVICE_HOSPITAL")));
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.isActivated").value(false))
                .andExpect(jsonPath("$.user.role").value(4));

        account.setRole((short) 5);
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value(5));
    }

    @Test
    void missingUserRetains404Envelope() throws Exception {
        given(users.findById(ID)).willReturn(Optional.empty());
        mvc.perform(get("/internal/users/" + ID).with(jwt().authorities(createAuthorityList("SERVICE_HOSPITAL"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void malformedIdDoesNotCallService() throws Exception {
        // Preserve the shared exception handler's current response; changing it affects external APIs too.
        mvc.perform(get("/internal/users/not-a-uuid").with(jwt().authorities(createAuthorityList("SERVICE_HOSPITAL")))).andExpect(status().isInternalServerError());
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
        given(users.findById(ID)).willReturn(Optional.of(user()));
        mvc.perform(get("/admin/users/" + ID).with(jwt().authorities(createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.id").value(ID.toString()))
                .andExpect(jsonPath("$.data.user.role").doesNotExist());
    }

    @Test
    void otherInternalPathsRemainProtected() throws Exception {
        mvc.perform(get("/internal/other")).andExpect(status().isUnauthorized());
    }
}
