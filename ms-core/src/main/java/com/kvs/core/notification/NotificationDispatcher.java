package com.kvs.core.notification;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes a message to the sender registered for its channel. Services inject this instead of a provider SDK.
 */
public class NotificationDispatcher {

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public NotificationDispatcher(List<NotificationSender> senders) {
        for (NotificationSender sender : senders) {
            if (this.senders.put(sender.channel(), sender) != null) {
                throw new IllegalStateException("More than one notification sender for " + sender.channel());
            }
        }
    }

    /** @throws NotificationDispatchException when no sender exists for the channel or the provider rejects it */
    public String send(NotificationChannel channel, NotificationMessage message) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new NotificationDispatchException("No notification sender configured for " + channel);
        }
        return sender.send(message);
    }
}
