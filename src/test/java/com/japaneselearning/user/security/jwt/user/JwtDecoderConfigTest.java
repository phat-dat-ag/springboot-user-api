package com.japaneselearning.user.security.jwt.user;

import com.japaneselearning.user.logging.filter.RequestLoggingFilter;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JwtDecoderConfigTest {
    private static final String URI = "https://jwks.example/api/v1/jwks/user";
    private static final String TRACE = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void correlationFilterRunsBeforeSecurityAndCleansRequestContext() throws Exception {
        var order = RequestLoggingFilter.class.getAnnotation(Order.class);

        assertTrue(order.value() < -100); // Spring Boot's default security filter order

        var response = new MockHttpServletResponse();

        new RequestLoggingFilter().doFilter(new MockHttpServletRequest(), response,
                (request, servletResponse) -> {
                    String id = MDC.get(RequestLoggingFilter.CORRELATION_ID);

                    assertNotNull(id);
                    assertEquals(id, response.getHeader(RequestLoggingFilter.HEADER_NAME));
                    assertDoesNotThrow(() -> UUID.fromString(id));
                });

        assertNull(MDC.get(RequestLoggingFilter.CORRELATION_ID));
    }

    @Test
    void decoderSendsCallerAndCurrentCorrelationAndPreservesBuilderTracing() throws Exception {
        MDC.put(RequestLoggingFilter.CORRELATION_ID, "request-123");

        decodeWithHeaders("user-service", "user-pod-7", "1.2.3", "request-123");

        assertEquals("request-123", MDC.get(RequestLoggingFilter.CORRELATION_ID));
    }

    @Test
    void generatesCorrelationWithoutLeakingMdc() throws Exception {
        decodeWithHeaders("user-service", "user-pod-7", "1.2.3", null);

        assertNull(MDC.get(RequestLoggingFilter.CORRELATION_ID));
    }

    @Test
    void invalidMetadataUsesContractSafeFallbacks() throws Exception {
        MDC.put(RequestLoggingFilter.CORRELATION_ID, "bad\r\nvalue");

        decodeWithHeaders("invalid service", "a".repeat(65), "", null);

        assertEquals("bad\r\nvalue", MDC.get(RequestLoggingFilter.CORRELATION_ID));
    }

    private void decodeWithHeaders(
            String service,
            String instance,
            String version,
            String correlation
    ) throws Exception {
        var key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        var jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
                new JWTClaimsSet.Builder()
                        .issuer("test-issuer")
                        .subject("test-user")
                        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                        .build()
        );

        jwt.sign(new RSASSASigner(key));

        AtomicReference<MockRestServiceServer> server = new AtomicReference<>();
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().set("traceparent", TRACE);
                    request.getHeaders().set("tracestate", "vendor=value");

                    return execution.execute(request, body);
                })
                .additionalCustomizers(template -> server.set(MockRestServiceServer.bindTo(template).build()));

        var decoder = new JwtDecoderConfig().jwtDecoder(
                new JwtProperties("test-issuer", 900, 30),
                URI,
                builder,
                service,
                instance,
                version
        );

        server.get().expect(requestTo(URI))
                .andExpect(header("X-Caller-Service", service.equals("user-service") ? service : "unknown"))
                .andExpect(header("X-Caller-Instance", instance.length() <= 64 ? instance : "unknown"))
                .andExpect(header("X-Caller-Version", version.isEmpty() ? "unknown" : version))
                .andExpect(header("traceparent", TRACE))
                .andExpect(header("tracestate", "vendor=value"))
                .andExpect(request -> {
                    var values = request.getHeaders().get("X-Correlation-ID");

                    assertNotNull(values);
                    assertEquals(1, values.size());

                    if (correlation != null)
                        assertEquals(correlation, values.get(0));

                    else assertEquals(values.get(0), UUID.fromString(values.get(0)).toString());
                })
                .andRespond(withSuccess(
                        new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON
                ));

        assertEquals("test-user", decoder.decode(jwt.serialize()).getSubject());

        server.get().verify();
    }
}
