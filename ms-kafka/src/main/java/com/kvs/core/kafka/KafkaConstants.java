package com.kvs.core.kafka;

/**
 * Common constants for Kafka topics, headers, and configuration across microservices.
 */
public final class KafkaConstants {

    private KafkaConstants() {
        // Utility class
    }

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";
}
