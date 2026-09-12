package com.prospecta.shared.exception;

import com.prospecta.shared.dto.ApiErrorResponse;
import com.prospecta.shared.security.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Business exception [{}] at {}: {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }
        log.warn("Validation failed for {}: {}", request.getRequestURI(), details);
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("VALIDATION_FAILED", "Request validation failed", traceId, details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Access denied for {} at {}: {}", TenantContextHolder.getUserPrincipal().map(p -> p.getEmail()).orElse("anonymous"), request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("FORBIDDEN", "Access is denied", traceId));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("UNAUTHORIZED", "Full authentication is required to access this resource", traceId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("BAD_REQUEST", ex.getMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        String paramName = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";
        log.warn("Parameter type mismatch for [{}] at {}: value '{}' cannot be converted to {}",
                paramName, request.getRequestURI(), value, ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type");

        if (UUID.class.equals(ex.getRequiredType())) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.of("RESOURCE_NOT_FOUND", "Resource with identifier '" + value + "' not found", traceId));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("INVALID_PARAMETER", "Invalid format for parameter '" + paramName + "': " + value, traceId));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", "The requested endpoint does not exist: " + request.getRequestURI(), traceId));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Method not allowed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiErrorResponse.of("METHOD_NOT_ALLOWED", ex.getMessage(), traceId));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("Malformed request payload at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("MALFORMED_REQUEST", "Malformed JSON body or unreadable request payload", traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.error("Unhandled internal server error [traceId={}] at {}: {}", traceId, request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "An unexpected server error occurred. Please contact support with the traceId.", traceId));
    }

    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        traceId = TenantContextHolder.getTraceId();
        if (traceId != null && !traceId.isBlank() && !"N/A".equals(traceId)) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }
}
