package com.japaneselearning.user.security.config;

import com.japaneselearning.user.security.jwt.user.JwtRoleConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Second security chain: applies user-token authentication and route permissions
 * to requests not matched by the internal API chain.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class UserSecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;

    // All routes not matched by the internal chain use user-token authentication.
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("jwtDecoder") JwtDecoder decoder
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to("health")).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/.well-known/**"
                        ).permitAll()

                        .requestMatchers(
                                "/admin/users/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(decoder)
                                .jwtAuthenticationConverter(jwtRoleConverterAdapter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtRoleConverterAdapter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);

        return converter;
    }
}
