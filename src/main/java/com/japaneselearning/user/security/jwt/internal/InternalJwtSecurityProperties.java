package com.japaneselearning.user.security.jwt.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.internal-jwt")
public record InternalJwtSecurityProperties(
        @NotBlank String jwkSetUri,
        @NotBlank String issuer,
        @NotBlank String subject,
        @NotBlank String audience,
        @NotNull Duration clockSkew,
        @NotNull Duration maxLifetime
) {
    public InternalJwtSecurityProperties {
        if (clockSkew != null
                && (clockSkew.isNegative() || clockSkew.compareTo(Duration.ofSeconds(30)) > 0)
        ) {
            throw new IllegalArgumentException(
                    "Internal JWT clock skew must be between 0 and 30 seconds"
            );
        }

        if (maxLifetime != null
                && (maxLifetime.compareTo(Duration.ofSeconds(1)) < 0
                || maxLifetime.compareTo(Duration.ofMinutes(5)) > 0)
        ) {
            throw new IllegalArgumentException(
                    "Internal JWT maximum lifetime must be between 1 and 300 seconds"
            );
        }
    }
}
