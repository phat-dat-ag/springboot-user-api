package com.japaneselearning.user.controller;

import com.japaneselearning.user.security.RsaKeyProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@RestController
@AllArgsConstructor
public class JwksController {

    private final RsaKeyProvider keyProvider;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksResponse> getJwks() {
        List<Jwk> jwks = keyProvider.getKeys()
                .values()
                .stream()
                .map(key -> {
                    var publicKey = key.publicKey();

                    return new Jwk(
                            "RSA",
                            "sig",
                            "RS256",
                            key.kid(),
                            encodeUnsigned(publicKey.getModulus()),
                            encodeUnsigned(publicKey.getPublicExponent())
                    );
                })
                .toList();

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()
                )
                .body(new JwksResponse(jwks));
    }

    private String encodeUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();

        // BigInteger.toByteArray() may prepend a sign byte.
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(
                    bytes, 1, bytes.length
            );
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public record JwksResponse(List<Jwk> keys) {
    }

    public record Jwk(
            String kty,
            String use,
            String alg,
            String kid,
            String n,
            String e
    ) {
    }
}
