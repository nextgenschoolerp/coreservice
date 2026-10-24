package com.kvs.core.notification;

/** Thrown when a provider rejects a message or cannot be reached. */
public class NotificationDispatchException extends RuntimeException {

    public NotificationDispatchException(String message) {
        super(message);
    }

    public NotificationDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
