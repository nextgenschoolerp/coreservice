package com.kvs.core.notification;

/** A provider for one {@link NotificationChannel}. */
public interface NotificationSender {

    NotificationChannel channel();

    /**
     * Sends the message.
     *
     * @return the provider's message identifier
     * @throws NotificationDispatchException when the provider rejects the message
     */
    String send(NotificationMessage message);
}
