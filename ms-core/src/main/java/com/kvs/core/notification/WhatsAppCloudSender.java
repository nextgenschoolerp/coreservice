package com.kvs.core.notification;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WhatsApp Business messaging through Meta's WhatsApp Cloud API (Graph API).
 * Free-form text is only delivered inside the 24-hour customer-service window. Anything sent to a parent or staff
 * member outside it must be an approved template, which is sent when {@link NotificationMessage#templateName()} is set.
 */
public class WhatsAppCloudSender implements NotificationSender {

    private final RestClient client;
    private final WhatsAppCloudSettings settings;

    public WhatsAppCloudSender(RestClient.Builder builder, WhatsAppCloudSettings settings) {
        this.settings = settings;
        this.client = builder
                .baseUrl("https://graph.facebook.com/" + settings.apiVersion())
                .defaultHeader("Authorization", "Bearer " + settings.accessToken())
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String send(NotificationMessage message) {
        Map<String, Object> body = message.templateName() != null
                ? templatePayload(message)
                : Map.of(
                        "messaging_product", "whatsapp",
                        "to", digits(message.recipient()),
                        "type", "text",
                        "text", Map.of("body", message.body()));
        try {
            Map<String, Object> response = client.post()
                    .uri("/{phoneNumberId}/messages", settings.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
            return String.valueOf(messages.get(0).get("id"));
        } catch (RestClientException | NullPointerException | IndexOutOfBoundsException e) {
            throw new NotificationDispatchException("WhatsApp Cloud API rejected the message: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> templatePayload(NotificationMessage message) {
        List<Map<String, Object>> parameters = new ArrayList<>();
        for (String value : message.templateParams()) {
            parameters.add(Map.of("type", "text", "text", value));
        }
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", message.templateName());
        template.put("language", Map.of("code", settings.templateLanguage()));
        if (!parameters.isEmpty()) {
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        return Map.of(
                "messaging_product", "whatsapp",
                "to", digits(message.recipient()),
                "type", "template",
                "template", template);
    }

    private static String digits(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    /** Settings bound from {@code kvs.notification.whatsapp.*}. */
    public record WhatsAppCloudSettings(String accessToken, String phoneNumberId, String apiVersion, String templateLanguage) {
    }
}
