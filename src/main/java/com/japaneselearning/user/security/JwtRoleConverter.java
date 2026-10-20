package com.japaneselearning.user.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Map<String, String> ROLE_MAPPING = Map.of(
            "1", "ROLE_USER",
            "2", "ROLE_ADMIN"
    );

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");

        if (role == null) {
            return List.of();
        }

        String authority = ROLE_MAPPING.get(role);

        if (authority == null) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority(authority));
    }
}