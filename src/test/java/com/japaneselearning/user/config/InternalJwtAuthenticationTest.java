package com.japaneselearning.user.config;

import com.japaneselearning.user.controller.UserManagementController;
import com.japaneselearning.user.controller.internal.InternalUserController;
import com.japaneselearning.user.dto.UserDTO;
import com.japaneselearning.user.dto.response.UserResponse;
import com.japaneselearning.user.security.JwtRoleConverter;
import com.japaneselearning.user.service.UserManagementService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import com.japaneselearning.user.security.JwtProperties;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({InternalUserController.class, UserManagementController.class})
@Import({SecurityConfig.class, JwtDecoderConfig.class, InternalJwtDecoderConfig.class, JwtRoleConverter.class,
        InternalJwtAuthenticationTest.ClientConfiguration.class})
class InternalJwtAuthenticationTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class ClientConfiguration {
        @Bean
        RestTemplateBuilder restTemplateBuilder() { return new RestTemplateBuilder(); }
    }
    private static final UUID ID = UUID.fromString("f05bf709-5cb8-4a4c-9ac4-e537b5579aef");
    private static final String INTERNAL = "/internal/users/" + ID;
    private static final RSAKey USER = key("user-test-v1");
    private static final RSAKey LEARNING = key("learning-test-v1");
    private static final RSAKey OTHER = key("other-key");
    private static final HttpServer SERVER = server();

    @Autowired
    MockMvc mvc;
    @MockitoBean
    UserManagementService users;

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + SERVER.getAddress().getPort();
        registry.add("app.security.jwt.jwk-set-uri", () -> base + "/api/v1/jwks/user");
        registry.add("app.security.jwt.issuer", () -> "user-service");
        registry.add("app.security.internal-jwt.jwk-set-uri", () -> base + "/api/v1/jwks/learning");
        registry.add("app.security.internal-jwt.issuer", () -> "learning-service");
        registry.add("app.security.internal-jwt.subject", () -> "learning-service");
        registry.add("app.security.internal-jwt.audience", () -> "user-service");
        registry.add("app.security.internal-jwt.clock-skew", () -> "5s");
        registry.add("app.security.internal-jwt.max-lifetime", () -> "300s");
    }

    @BeforeEach
    void response() {
        given(users.getUser(ID)).willReturn(new UserResponse(new UserDTO(ID, "test-user", "test@example.invalid",
                true, null, null)));
    }

    @AfterAll
    static void stop() { SERVER.stop(0); }

    @Test
    void validLearningIdentityCanReadInternalUser() throws Exception {
        mvc.perform(get(INTERNAL).header("Authorization", "Bearer " + token(LEARNING, claims(), "internal+jwt", LEARNING.getKeyID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(ID.toString()));
    }

    @Test
    void anonymousInternalRequestIsRejected() throws Exception {
        mvc.perform(get(INTERNAL)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"issuer", "audience", "subject", "type", "signature", "kid", "noKid",
            "noIssuer", "noSubject", "noAudience", "noExpiry", "noIssuedAt", "noJti", "expired",
            "futureIssuedAt", "futureNotBefore", "longLifetime", "invertedTimes", "algorithm"})
    void invalidInternalTokensAreRejected(String problem) throws Exception {
        var claims = claims();
        String type = "internal+jwt";
        String kid = LEARNING.getKeyID();
        RSAKey signer = LEARNING;
        Instant now = Instant.now();
        switch (problem) {
            case "issuer" -> claims.issuer("untrusted");
            case "audience" -> claims.audience("another-service");
            case "subject" -> claims.subject("another-service");
            case "type" -> type = "JWT";
            case "signature" -> signer = OTHER;
            case "kid" -> kid = "unknown";
            case "noKid" -> kid = null;
            case "noIssuer" -> claims.issuer(null);
            case "noSubject" -> claims.subject(null);
            case "noAudience" -> claims.audience((String) null);
            case "noExpiry" -> claims.expirationTime(null);
            case "noIssuedAt" -> claims.issueTime(null);
            case "noJti" -> claims.jwtID(null);
            case "expired" -> claims.issueTime(Date.from(now.minusSeconds(120))).expirationTime(Date.from(now.minusSeconds(60)));
            case "futureIssuedAt" -> claims.issueTime(Date.from(now.plusSeconds(30))).expirationTime(Date.from(now.plusSeconds(90)));
            case "futureNotBefore" -> claims.notBeforeTime(Date.from(now.plusSeconds(30)));
            case "longLifetime" -> claims.expirationTime(Date.from(now.plusSeconds(301)));
            case "invertedTimes" -> claims.expirationTime(Date.from(now.minusSeconds(1)));
        }
        String token;
        if (problem.equals("algorithm")) {
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS512).type(new JOSEObjectType(type)).keyID(kid).build(), claims.build());
            jwt.sign(new RSASSASigner(signer));
            token = jwt.serialize();
        } else {
            token = token(signer, claims, type, kid);
        }
        mvc.perform(get(INTERNAL).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void keySetsAndSecurityChainsRemainIsolated() throws Exception {
        String user = token(USER, userClaims(1), "JWT", USER.getKeyID());
        String internal = token(LEARNING, claims(), "internal+jwt", LEARNING.getKeyID());
        mvc.perform(get(INTERNAL).header("Authorization", "Bearer " + user)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/users/" + ID).header("Authorization", "Bearer " + internal)).andExpect(status().isUnauthorized());
        // Even spoofed user claims signed by Learning cannot pass the external User-only key source.
        mvc.perform(get("/admin/users/" + ID).header("Authorization", "Bearer " +
                token(LEARNING, userClaims(2), "JWT", LEARNING.getKeyID()))).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/users/" + ID).header("Authorization", "Bearer " + user)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users/" + ID).header("Authorization", "Bearer " +
                token(USER, userClaims(2), "JWT", USER.getKeyID()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post(INTERNAL).header("Authorization", "Bearer " + internal)).andExpect(status().isForbidden());
        mvc.perform(get("/internal/other").header("Authorization", "Bearer " + internal)).andExpect(status().isForbidden());
    }

    private static JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer("learning-service").subject("learning-service").audience("user-service")
                .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(60))).jwtID(UUID.randomUUID().toString());
    }

    private static JWTClaimsSet.Builder userClaims(int role) {
        return claims().issuer("user-service").subject(ID.toString()).claim("role", role);
    }

    private static String token(RSAKey key, JWTClaimsSet.Builder claims, String type, String kid) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType(type)).keyID(kid).build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static RSAKey key(String kid) {
        try { return new RSAKeyGenerator(2048).keyID(kid).generate(); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private static HttpServer server() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            for (var owner : new String[]{"user", "learning"}) {
                byte[] body = new JWKSet((owner.equals("user") ? USER : LEARNING).toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                server.createContext("/api/v1/jwks/" + owner, exchange -> {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (var out = exchange.getResponseBody()) { out.write(body); }
                });
            }
            server.start();
            return server;
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
