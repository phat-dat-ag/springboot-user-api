package com.japaneselearning.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Duration;
import java.time.Instant;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InternalJwtSecurityProperties.class)
public class InternalJwtDecoderConfig {
    @Bean("internalJwtDecoder")
    public JwtDecoder internalJwtDecoder(
            InternalJwtSecurityProperties properties,
            RestTemplateBuilder builder
    ) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
                .restOperations(
                        builder.connectTimeout(Duration.ofSeconds(3))
                                .readTimeout(Duration.ofSeconds(5))
                                .build()
                )
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                // Nimbus's default JWT type check does not accept internal+jwt. Enforce it explicitly below.
                .validateType(false)
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(properties.clockSkew()),
                new JwtIssuerValidator(properties.issuer()),
                jwt -> {
                    Object kid = jwt.getHeaders().get("kid");
                    Instant issuedAt = jwt.getIssuedAt();
                    Instant expiresAt = jwt.getExpiresAt();

                    if (!"internal+jwt".equals(jwt.getHeaders().get("typ"))
                            || !(kid instanceof String id)
                            || id.isBlank()
                            || !properties.subject().equals(jwt.getSubject())
                            || jwt.getAudience() == null
                            || !jwt.getAudience().contains(properties.audience())
                            || jwt.getId() == null
                            || jwt.getId().isBlank()
                            || issuedAt == null
                            || expiresAt == null
                            || issuedAt.isAfter(Instant.now().plus(properties.clockSkew()))
                            || !expiresAt.isAfter(issuedAt)
                            || Duration.between(issuedAt, expiresAt).compareTo(properties.maxLifetime()) > 0
                    ) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token",
                                "Invalid internal token",
                                null
                        ));
                    }

                    return OAuth2TokenValidatorResult.success();
                }
        ));

        return decoder;
    }
}
