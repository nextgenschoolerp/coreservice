package com.kvs.core.kafka;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseEventTest {

    @Test
    void testEventCreation() {
        BaseEvent<String> event = BaseEvent.of("USER_CREATED", "userservice", "payload123");
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertEquals("USER_CREATED", event.getEventType());
        assertEquals("userservice", event.getSourceService());
        assertEquals("payload123", event.getPayload());
    }
}
