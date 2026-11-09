package com.japaneselearning.user.security.jwks;

import com.japaneselearning.user.common.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds diagnostic caller metadata to outbound JWKS requests.
 * These self-reported headers are not authentication credentials.
 * Instrumentation's trace headers are left unchanged.
 */
public final class CallerMetadataInterceptor implements ClientHttpRequestInterceptor {
    private final String service;
    private final String instance;
    private final String version;

    public CallerMetadataInterceptor(String service, String instance, String version) {
        this.service = identity(service);
        this.instance = identity(instance);
        this.version = identity(version);
    }

    private static boolean valid(String value) {
        return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    }

    private static String identity(String value) {
        return valid(value) ? value : "unknown";
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        var headers = request.getHeaders();

        headers.set("X-Caller-Service", service);
        headers.set("X-Caller-Instance", instance);
        headers.set("X-Caller-Version", version);

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID);

        headers.set(CorrelationIdFilter.HEADER_NAME,
                valid(correlationId) ? correlationId : UUID.randomUUID().toString()
        );

        return execution.execute(request, body);
    }
}
