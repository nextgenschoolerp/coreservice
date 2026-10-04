package com.kvs.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope for client and server errors across microservices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    @Builder.Default
    private boolean success = false;

    private int status;

    private String errorCode;

    private String message;

    private String path;

    @Builder.Default
    private String traceId = resolveCurrentTraceId();

    @Builder.Default
    private Instant timestamp = Instant.now();

    private List<ValidationError> validationErrors;

    private static String resolveCurrentTraceId() {
        String customTraceId = com.kvs.core.config.tracing.TraceCorrelationFilter.getCustomTraceId();
        if (customTraceId != null && !customTraceId.isBlank()) {
            return customTraceId;
        }
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : null;
    }
}
