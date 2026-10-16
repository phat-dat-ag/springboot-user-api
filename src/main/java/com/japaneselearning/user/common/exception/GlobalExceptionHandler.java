package com.japaneselearning.user.common.exception;

import com.japaneselearning.user.common.response.APIError;
import com.japaneselearning.user.common.response.APIResponse;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Business exceptions
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIResponse<Void>> handleBusinessException(
            BusinessException ex
    ) {
        ErrorCode errorCode = ex.getErrorCode();

        return buildErrorResponse(
                errorCode,
                ex.getMessage(),
                null
        );
    }

    // 2. @Valid request body validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        List<APIError.ErrorDetail> details =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(fieldError -> new APIError.ErrorDetail(
                                fieldError.getField(),
                                "INVALID_FIELD",
                                fieldError.getDefaultMessage() != null
                                        ? fieldError.getDefaultMessage()
                                        : "Invalid value"
                        ))
                        .toList();

        return buildErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getMessage(),
                new APIError(details)
        );
    }

    // 3. Method-level or parameter validation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        List<APIError.ErrorDetail> details =
                ex.getConstraintViolations()
                        .stream()
                        .map(violation -> new APIError.ErrorDetail(
                                violation.getPropertyPath().toString(),
                                "INVALID_FIELD",
                                violation.getMessage()
                        ))
                        .toList();

        return buildErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getMessage(),
                new APIError(details)
        );
    }

    // 4. Invalid JSON or unreadable request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Void>> handleInvalidRequestBody(
            HttpMessageNotReadableException ex
    ) {
        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST,
                ErrorCode.INVALID_REQUEST.getMessage(),
                null
        );
    }

    // 5. Unsupported HTTP method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex
    ) {
        return buildErrorResponse(
                ErrorCode.METHOD_NOT_ALLOWED,
                ErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                null
        );
    }

    // 6. Resource or endpoint not found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleResourceNotFound(
            NoResourceFoundException ex
    ) {
        return buildErrorResponse(
                ErrorCode.RESOURCE_NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND.getMessage(),
                null
        );
    }

    // 7. Database integrity errors not mapped elsewhere
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        log.warn("Database integrity constraint violation");

        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST,
                "The request conflicts with existing data",
                null
        );
    }

    // 8. Unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleUnexpectedException(
            Exception ex
    ) {
        log.error("Unexpected application error", ex);

        return buildErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                null
        );
    }

    // Shared response builder
    private ResponseEntity<APIResponse<Void>> buildErrorResponse(
            ErrorCode errorCode,
            String message,
            APIError error
    ) {
        APIResponse<Void> response = APIResponse.failure(
                errorCode.name(),
                message,
                error
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }
}