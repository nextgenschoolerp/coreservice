package com.kvs.core.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard envelope for asynchronous Kafka domain events across microservices.
 *
 * @param <T> Event payload data type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseEvent<T> {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;

    private String sourceService;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String traceId = resolveCurrentTraceId();

    private T payload;

    public static <T> BaseEvent<T> of(String eventType, String sourceService, T payload) {
        return BaseEvent.<T>builder()
                .eventType(eventType)
                .sourceService(sourceService)
                .payload(payload)
                .build();
    }

    private static String resolveCurrentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : null;
    }
}
