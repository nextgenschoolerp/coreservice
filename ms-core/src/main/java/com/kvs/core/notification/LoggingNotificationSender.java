package com.kvs.core.notification;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/** Default sender: logs the message and returns a fake id. Used when a provider is not configured. */
@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    private final NotificationChannel channel;

    public LoggingNotificationSender(NotificationChannel channel) {
        this.channel = channel;
    }

    @Override
    public NotificationChannel channel() {
        return channel;
    }

    @Override
    public String send(NotificationMessage message) {
        String id = "LOG-" + channel + "-" + UUID.randomUUID();
        log.info("[{} stub] to={} id={} subject={} body={}", channel, mask(message.recipient()), id, message.subject(), message.body());
        return id;
    }

    static String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
