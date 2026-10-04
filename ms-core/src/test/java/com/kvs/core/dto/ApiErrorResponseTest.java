package com.kvs.core.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MDC.clear();
    }

    @Test
    @DisplayName("ApiErrorResponse defaults success to false and generates timestamp")
    void testDefaults() {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(400)
                .errorCode("BAD_REQUEST")
                .message("Invalid parameter")
                .path("/api/v1/test")
                .build();

        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatus());
        assertEquals("BAD_REQUEST", response.getErrorCode());
        assertEquals("Invalid parameter", response.getMessage());
        assertEquals("/api/v1/test", response.getPath());
        assertNotNull(response.getTimestamp());
        assertNull(response.getTraceId());
    }

    @Test
    @DisplayName("ApiErrorResponse captures active traceId from MDC")
    void testTraceIdFromMdc() {
        MDC.put("traceId", "trace-abc-123");

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(500)
                .errorCode("INTERNAL_ERROR")
                .message("Server error")
                .build();

        assertEquals("trace-abc-123", response.getTraceId());
    }

    @Test
    @DisplayName("ApiErrorResponse captures active traceId from TraceCorrelationFilter")
    void testTraceIdFromTraceCorrelationFilter() {
        com.kvs.core.config.tracing.TraceCorrelationFilter.setCustomTrace("trace-filter-456", "span-filter-789");
        try {
            ApiErrorResponse response = ApiErrorResponse.builder()
                    .status(404)
                    .errorCode("NOT_FOUND")
                    .message("Resource not found")
                    .build();

            assertEquals("trace-filter-456", response.getTraceId());
        } finally {
            com.kvs.core.config.tracing.TraceCorrelationFilter.clearCustomTrace();
        }
    }

    @Test
    @DisplayName("ApiErrorResponse supports validation errors list")
    void testValidationErrors() {
        ValidationError error = ValidationError.builder()
                .field("email")
                .rejectedValue("invalid-email")
                .message("Must be a well-formed email address")
                .build();

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(400)
                .errorCode("VALIDATION_FAILED")
                .message("Validation failed")
                .validationErrors(List.of(error))
                .build();

        assertNotNull(response.getValidationErrors());
        assertEquals(1, response.getValidationErrors().size());
        assertEquals("email", response.getValidationErrors().get(0).getField());
        assertEquals("invalid-email", response.getValidationErrors().get(0).getRejectedValue());
    }

    @Test
    @DisplayName("ApiErrorResponse serializes cleanly to JSON")
    void testJsonSerialization() throws Exception {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(404)
                .errorCode("RESOURCE_NOT_FOUND")
                .message("User not found with id : '99'")
                .path("/api/v1/profile/99")
                .timestamp(Instant.parse("2026-10-04T12:00:00Z"))
                .traceId("trace-xyz-999")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"status\":404"));
        assertTrue(json.contains("\"errorCode\":\"RESOURCE_NOT_FOUND\""));
        assertTrue(json.contains("\"message\":\"User not found with id : '99'\""));
        assertTrue(json.contains("\"path\":\"/api/v1/profile/99\""));
        assertTrue(json.contains("\"traceId\":\"trace-xyz-999\""));
    }
}
