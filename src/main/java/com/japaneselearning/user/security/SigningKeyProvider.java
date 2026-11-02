package com.japaneselearning.user.security;

import com.japaneselearning.user.config.SigningKeyProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads this service's private signing keys; verification keys come from remote JWKS.
 */
@Component
@RequiredArgsConstructor
public class SigningKeyProvider {

    private final SigningKeyProperties properties;
    private SigningKey activeSigningKey;

    @PostConstruct
    public void loadKeys() {
        if (properties.getKeys() == null || properties.getKeys().isEmpty()) {
            throw new IllegalStateException(
                    "At least one RSA signing key must be configured"
            );
        }

        Set<String> keyIds = new HashSet<>();
        SigningKey activeKey = null;

        for (SigningKeyProperties.Key config : properties.getKeys()) {
            if (config == null
                    || config.getKid() == null
                    || config.getKid().isBlank()
            ) {
                throw new IllegalStateException(
                        "RSA signing key kid must not be blank"
                );
            }

            if (!keyIds.add(config.getKid())) {
                throw new IllegalStateException(
                        "Duplicate RSA signing key kid: " + config.getKid()
                );
            }

            if (config.getPrivateKeyPath() == null
                    || config.getPrivateKeyPath().isBlank()
            ) {
                throw new IllegalStateException(
                        "RSA private key path must not be blank for kid: "
                                + config.getKid()
                );
            }

            RSAPrivateKey privateKey;

            try {
                privateKey = readPrivateKey(config.getPrivateKeyPath());
            } catch (IOException
                     | GeneralSecurityException
                     | IllegalArgumentException exception
            ) {
                throw new IllegalStateException(
                        "Failed to load RSA signing key: " + config.getKid(),
                        exception
                );
            }

            if (config.isActive()) {
                if (activeKey != null) {
                    throw new IllegalStateException(
                            "Exactly one RSA signing key must be active"
                    );
                }

                activeKey = new SigningKey(config.getKid(), privateKey);
            }
        }

        if (activeKey == null) {
            throw new IllegalStateException(
                    "Exactly one RSA signing key must be active"
            );
        }

        activeSigningKey = activeKey;
    }

    public SigningKey getActiveSigningKey() {
        if (activeSigningKey == null) {
            throw new IllegalStateException("RSA signing keys have not been loaded");
        }

        return activeSigningKey;
    }

    private RSAPrivateKey readPrivateKey(String filePath)
            throws IOException, GeneralSecurityException {

        String content = Files.readString(Path.of(filePath))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] bytes = Base64.getDecoder().decode(content);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public record SigningKey(
            String kid,
            RSAPrivateKey privateKey
    ) {
    }
}
