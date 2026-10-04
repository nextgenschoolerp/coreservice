package com.kvs.core.kafka;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConstantsTest {

    @Test
    void testKafkaConstants() {
        assertNotNull(KafkaConstants.TRACE_ID_HEADER);
        assertNotNull(KafkaConstants.SPAN_ID_HEADER);
    }
}
