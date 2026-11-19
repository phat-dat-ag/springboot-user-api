package com.hospitalmanagement.user.security.jwt.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @ParameterizedTest
    @CsvSource({
            "1, ROLE_USER",
            "2, ROLE_PATIENT",
            "3, ROLE_STAFF",
            "4, ROLE_DOCTOR",
            "5, ROLE_ADMIN"
    })
    void mapsNumericRoleToExactlyOneAuthority(short role, String authority) {
        Jwt jwt = token().claim("role", role).build();

        assertThat(converter.convert(jwt))
                .containsExactly(new SimpleGrantedAuthority(authority));
    }

    @Test
    void grantsNoAuthorityForUnknownRole() {
        Jwt jwt = token().claim("role", 99).build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void grantsNoAuthorityWhenRoleIsMissing() {
        Jwt jwt = token().build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt.Builder token() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test-user");
    }
}
