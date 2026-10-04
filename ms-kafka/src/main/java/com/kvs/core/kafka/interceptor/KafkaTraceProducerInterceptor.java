package com.kvs.core.kafka.interceptor;

import com.kvs.core.kafka.KafkaConstants;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka ProducerInterceptor that automatically injects traceId and spanId headers into outgoing records.
 */
public class KafkaTraceProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    @Override
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");

        Headers headers = record.headers();
        if (traceId != null && !traceId.isBlank() && headers.lastHeader(KafkaConstants.TRACE_ID_HEADER) == null) {
            headers.add(KafkaConstants.TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }
        if (spanId != null && !spanId.isBlank() && headers.lastHeader(KafkaConstants.SPAN_ID_HEADER) == null) {
            headers.add(KafkaConstants.SPAN_ID_HEADER, spanId.getBytes(StandardCharsets.UTF_8));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // No-op
    }
}
