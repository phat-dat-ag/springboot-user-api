package com.japaneselearning.user.config;

import com.japaneselearning.user.security.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(
            JwtProperties jwtProperties,
            @Value("${app.security.jwt.jwk-set-uri}") String jwkSetUri,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.security.jwks.caller-service}") String service,
            @Value("${app.security.jwks.caller-instance}") String instance,
            @Value("${app.security.jwks.caller-version}") String version
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .restOperations(restTemplateBuilder.additionalInterceptors(
                                new JwksCallerInterceptor(service, instance, version)
                        ).build()
                )
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        jwtProperties.issuer()
                )
        );

        return decoder;
    }
}
