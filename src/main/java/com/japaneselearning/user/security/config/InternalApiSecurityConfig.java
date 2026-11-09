package com.japaneselearning.user.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;

import java.util.List;

/**
 * First security chain: authenticates Learning service tokens for internal routes.
 * Only the explicitly listed internal operations are permitted.
 */
@Configuration
public class InternalApiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,

            @Qualifier("internalJwtDecoder")
            JwtDecoder decoder
    ) throws Exception {
        var converter = new JwtAuthenticationConverter();

        // The decoder has verified the configured Learning service identity.
        // Grant its authority here rather than trusting roles supplied in a token.
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of(
                new SimpleGrantedAuthority("SERVICE_LEARNING")
        ));

        // This grants service-level access; end-user ownership is not checked here.
        return http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET, "/internal/users/*"
                        ).hasAuthority("SERVICE_LEARNING")
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                                jwt.decoder(decoder).jwtAuthenticationConverter(converter)
                        )
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
                                var handler = new AuthenticationEntryPointFailureHandler(
                                        new BearerTokenAuthenticationEntryPoint()
                                );

                                handler.setRethrowAuthenticationServiceException(false);
                                filter.setAuthenticationFailureHandler(handler);

                                return filter;
                            }
                        }))
                .build();
    }
}
