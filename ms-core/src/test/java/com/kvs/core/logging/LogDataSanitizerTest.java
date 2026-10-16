package com.kvs.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogDataSanitizerTest {

    private final LogDataSanitizer sanitizer = new LogDataSanitizer(new ObjectMapper());

    @Test
    void redactsSensitiveFieldsRecursively() {
        String sanitized = sanitizer.sanitize(Map.of(
                "username", "teacher",
                "password", "dont-log-this",
                "nested", Map.of("access_token", "also-secret", "count", 2)
        ));

        assertTrue(sanitized.contains("teacher"));
        assertTrue(sanitized.contains("[REDACTED]"));
        assertFalse(sanitized.contains("dont-log-this"));
        assertFalse(sanitized.contains("also-secret"));
    }

    @Test
    void redactsQueryParameterValues() {
        String sanitized = sanitizer.sanitizeUrl(
                "https://example.test/api/users",
                "tenantId=tenant-1&access_token=secret"
        );

        assertTrue(sanitized.contains("tenantId=[REDACTED]"));
        assertTrue(sanitized.contains("access_token=[REDACTED]"));
        assertFalse(sanitized.contains("tenant-1"));
        assertFalse(sanitized.contains("secret"));
    }

    @Test
    void truncatesLargeLogValues() {
        String sanitized = sanitizer.sanitize(Map.of("details", "x".repeat(9_000)));

        assertTrue(sanitized.endsWith("...[TRUNCATED]"));
        assertTrue(sanitized.length() < 8_100);
    }
}
