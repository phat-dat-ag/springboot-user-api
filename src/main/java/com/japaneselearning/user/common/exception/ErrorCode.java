package com.japaneselearning.user.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Client errors
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed"),


    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user not found"),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    USER_USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "username is already registered"),

    USER_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username/ email or password"),
    USER_ACCOUNT_NOT_ACTIVATED(HttpStatus.FORBIDDEN, "Account is not activated"),

    AUTH_REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "refresh token not found"),
    AUTH_REFRESH_TOKEN_REVOKED(HttpStatus.FORBIDDEN, "refresh token has been already revoked"),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.FORBIDDEN, "refresh token has been already expired"),

    USER_MANAGEMENT_ACTIVATION_ACTIVATED(HttpStatus.CONFLICT, "user is activated, can not activate"),
    USER_MANAGEMENT_ACTIVATION_DEACTIVATED(HttpStatus.CONFLICT, "user is deactivated, can not deactivate"),

    // Server errors
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
    );

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}