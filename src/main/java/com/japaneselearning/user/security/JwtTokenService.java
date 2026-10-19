package com.japaneselearning.user.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.japaneselearning.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    private final RsaKeyProvider rsaKeyProvider;
    private final ObjectMapper objectMapper;

    public GeneratedAccessToken generateAccessToken(User user) {
        try {
//            get active key in key list
            RsaKeyProvider.KeyMaterial activeKey = getActiveSigningKey();

            Map<String, Object> header = createHeader(activeKey);

            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plusSeconds(jwtProperties.accessTokenTtlSeconds());

            Map<String, Object> claims = createClaims(user, issuedAt, expiresAt);

            String encodedHeader = encodeJson(header);
            String encodedClaims = encodeJson(claims);

            String signingInput = encodedHeader + "." + encodedClaims;

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(activeKey.privateKey());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));

            String encodedSignature = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature.sign());

            String token = signingInput + "." + encodedSignature;

            return new GeneratedAccessToken(
                    token,
                    jwtProperties.accessTokenTtlSeconds()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate access token",
                    exception
            );
        }
    }

    private RsaKeyProvider.KeyMaterial getActiveSigningKey() {
        return rsaKeyProvider.getKeys()
                .values()
                .stream()
                .filter(RsaKeyProvider.KeyMaterial::active)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No active RSA signing key configured"
                        )
                );
    }

    private Map<String, Object> createHeader(
            RsaKeyProvider.KeyMaterial key
    ) {
        return Map.of(
                "alg", "RS256",
                "typ", "JWT",
                "kid", key.kid()
        );
    }

    private Map<String, Object> createClaims(
            User user,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Map<String, Object> claims = new LinkedHashMap<>();

        claims.put("iss", jwtProperties.issuer());
        claims.put("sub", user.getId().toString());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("role", user.getRole());

        return claims;
    }

    private String encodeJson(Map<String, Object> value) throws JsonProcessingException {
        byte[] json = objectMapper.writeValueAsBytes(value);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json);
    }

    public record GeneratedAccessToken(
            String token,
            long expiresIn
    ) {
    }
}
