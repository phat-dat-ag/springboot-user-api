package com.japaneselearning.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.japaneselearning.user.config.SigningKeyProperties;
import com.japaneselearning.user.entity.User;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SigningKeyProviderTest {

    @TempDir
    Path directory;
    private KeyPair keyPair;
    private Path privateKeyPath;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        privateKeyPath = directory.resolve("private.pem");
        Files.writeString(privateKeyPath, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n");
    }

    @Test
    void bindsEnvironmentAndSignsWithoutPublicKeyFile() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "signing-test-systemEnvironment", Map.of(
                "APP_SECURITY_SIGNING_KEYS_0_KID", "user-key",
                "APP_SECURITY_SIGNING_KEYS_0_PRIVATE_KEY_PATH", privateKeyPath.toString(),
                "APP_SECURITY_SIGNING_KEYS_0_ACTIVE", "true"
        )));
        SigningKeyProperties properties = Binder.get(environment)
                .bind("app.security.signing", SigningKeyProperties.class).get();
        SigningKeyProvider provider = new SigningKeyProvider(properties);
        provider.loadKeys();

        JwtTokenService service = new JwtTokenService(
                new JwtProperties("user-service", 900, 30), provider, new ObjectMapper());
        User user = new User("user", "user@example.com", "unused");
        user.setId(UUID.randomUUID());
        var token = service.generateAccessToken(user);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("user-service"));
        var jwt = decoder.decode(token.token());
        assertEquals("user-key", jwt.getHeaders().get("kid"));
        assertEquals("RS256", jwt.getHeaders().get("alg"));
        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals("1", jwt.getClaimAsString("role"));
        assertNotNull(jwt.getId());
        assertEquals(900, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        assertEquals(900, token.expiresIn());
        assertEquals("JWT", SignedJWT.parse(token.token()).getHeader().getType().toString());
    }

    @Test
    void rejectsMissingOrMultipleActiveKeysAndDuplicateIds() {
        assertThrows(IllegalStateException.class, () -> provider(List.of()).loadKeys());
        assertThrows(IllegalStateException.class, () -> provider(List.of(key("a", false))).loadKeys());
        assertThrows(IllegalStateException.class,
                () -> provider(List.of(key("a", true), key("b", true))).loadKeys());
        assertThrows(IllegalStateException.class,
                () -> provider(List.of(key("a", true), key("a", false))).loadKeys());
    }

    @Test
    void rejectsInvalidPrivateKeyWithKeyIdInError() throws Exception {
        Files.writeString(privateKeyPath, "invalid PEM");
        var exception = assertThrows(IllegalStateException.class,
                () -> provider(List.of(key("broken-key", true))).loadKeys());
        assertTrue(exception.getMessage().contains("broken-key"));
    }

    private SigningKeyProperties.Key key(String kid, boolean active) {
        var key = new SigningKeyProperties.Key();
        key.setKid(kid);
        key.setPrivateKeyPath(privateKeyPath.toString());
        key.setActive(active);
        return key;
    }

    private SigningKeyProvider provider(List<SigningKeyProperties.Key> keys) {
        var properties = new SigningKeyProperties();
        properties.setKeys(keys);
        return new SigningKeyProvider(properties);
    }
}
