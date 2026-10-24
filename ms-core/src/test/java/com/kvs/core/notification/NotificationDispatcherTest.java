package com.kvs.core.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationDispatcherTest {

    @Test
    void routesEachChannelToItsSender() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(
                new LoggingNotificationSender(NotificationChannel.WHATSAPP),
                new LoggingNotificationSender(NotificationChannel.EMAIL)));

        String waId = dispatcher.send(NotificationChannel.WHATSAPP, new NotificationMessage("+919812345678", null, "Fee due"));
        String mailId = dispatcher.send(NotificationChannel.EMAIL, new NotificationMessage("parent@example.com", "Receipt", "Thanks"));

        assertThat(waId).startsWith("LOG-WHATSAPP-");
        assertThat(mailId).startsWith("LOG-EMAIL-");
    }

    @Test
    void rejectsChannelWithoutSender() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(new LoggingNotificationSender(NotificationChannel.EMAIL)));

        assertThatThrownBy(() -> dispatcher.send(NotificationChannel.WHATSAPP, new NotificationMessage("+919812345678", null, "Hi")))
                .isInstanceOf(NotificationDispatchException.class)
                .hasMessageContaining("WHATSAPP");
    }

    @Test
    void rejectsTwoSendersForOneChannel() {
        assertThatThrownBy(() -> new NotificationDispatcher(List.of(
                new LoggingNotificationSender(NotificationChannel.EMAIL),
                new LoggingNotificationSender(NotificationChannel.EMAIL))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void messageNeedsRecipientAndBody() {
        assertThatThrownBy(() -> new NotificationMessage(" ", null, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationMessage("a@b.c", null, "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loggingSenderMasksRecipient() {
        assertThat(LoggingNotificationSender.mask("+919812345678")).isEqualTo("*********5678");
        assertThat(LoggingNotificationSender.mask("1234")).isEqualTo("****");
    }
}
