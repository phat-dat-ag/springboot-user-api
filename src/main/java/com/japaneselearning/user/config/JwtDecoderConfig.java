package com.japaneselearning.user.config;

import com.japaneselearning.user.security.RsaKeyProvider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.japaneselearning.user.security.JwtProperties;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            RsaKeyProvider rsaKeyProvider,
            JwtProperties jwtProperties
    ) {
        List<RSAKey> rsaKeys = rsaKeyProvider.getKeys()
                .values()
                .stream()
                .map(key -> new RSAKey.Builder(key.publicKey())
                        .keyID(key.kid())
                        .build()
                )
                .toList();

        JWKSet jwkSet = new JWKSet(
                rsaKeys.stream()
                        .map(JWK.class::cast)
                        .toList()
        );

        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

        JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(
                        JWSAlgorithm.RS256,
                        jwkSource
                );

        processor.setJWSKeySelector(keySelector);

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        jwtProperties.issuer()
                )
        );

        return decoder;
    }
}