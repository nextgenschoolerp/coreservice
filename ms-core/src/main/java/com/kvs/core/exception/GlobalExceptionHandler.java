package com.kvs.core.exception;

import com.kvs.core.config.tracing.TraceCorrelationFilter;
import com.kvs.core.dto.ApiErrorResponse;
import com.kvs.core.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global REST exception handler that produces standardized ApiErrorResponse envelopes,
 * ensuring the traceId in the response payload strictly matches the X-Trace-Id response header.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {

    private String resolveAndSyncTraceId(HttpServletRequest request, HttpServletResponse response) {
        String traceId = null;
        if (response != null) {
            traceId = response.getHeader(TraceCorrelationFilter.TRACE_HEADER);
        }
        if (!StringUtils.hasText(traceId) && request != null) {
            traceId = (String) request.getAttribute(TraceCorrelationFilter.TRACE_HEADER);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = TraceCorrelationFilter.getCustomTraceId();
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = MDC.get(TraceCorrelationFilter.MDC_TRACE_ID);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        if (response != null) {
            response.setHeader(TraceCorrelationFilter.TRACE_HEADER, traceId);
        }
        if (request != null) {
            request.setAttribute(TraceCorrelationFilter.TRACE_HEADER, traceId);
        }
        MDC.put(TraceCorrelationFilter.MDC_TRACE_ID, traceId);
        TraceCorrelationFilter.setCustomTrace(traceId, TraceCorrelationFilter.getCustomSpanId());

        return traceId;
    }

    @ExceptionHandler(BaseApiException.class)
    public ResponseEntity<ApiErrorResponse> handleBaseApiException(
            BaseApiException ex, HttpServletRequest request, HttpServletResponse response) {
        log.warn("Domain exception occurred: status={}, code={}, message='{}'",
                ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());

        String traceId = resolveAndSyncTraceId(request, response);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(ex.getHttpStatus().value())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response) {
        List<ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(ValidationError.builder()
                    .field(fieldError.getField())
                    .rejectedValue(fieldError.getRejectedValue())
                    .message(fieldError.getDefaultMessage())
                    .build());
        }

        log.warn("Validation failed for URI '{}': {} field errors", request.getRequestURI(), validationErrors.size());

        String traceId = resolveAndSyncTraceId(request, response);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_FAILED")
                .message("Request validation failed")
                .path(request.getRequestURI())
                .traceId(traceId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request, HttpServletResponse response) {
        log.warn("Authentication failed for URI '{}': {}", request.getRequestURI(), ex.getMessage());

        String traceId = resolveAndSyncTraceId(request, response);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode("UNAUTHORIZED")
                .message(ex.getMessage() != null ? ex.getMessage() : "Full authentication is required to access this resource")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request, HttpServletResponse response) {
        log.warn("Access denied for URI '{}': {}", request.getRequestURI(), ex.getMessage());

        String traceId = resolveAndSyncTraceId(request, response);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode("ACCESS_DENIED")
                .message("Access is denied")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request, HttpServletResponse response) {
        log.warn("Response status exception: status={}, reason='{}'", ex.getStatusCode(), ex.getReason());

        String traceId = resolveAndSyncTraceId(request, response);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(ex.getStatusCode().value())
                .errorCode(ex.getStatusCode().toString())
                .message(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Unhandled internal server error for URI '{}'", request.getRequestURI(), ex);

        String traceId = resolveAndSyncTraceId(request, response);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected internal error occurred. Please contact support.")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
