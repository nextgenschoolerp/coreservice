package com.kvs.core.notification;

import java.util.List;

/**
 * One outbound message.
 *
 * @param recipient      WhatsApp: E.164 phone number (for example +919812345678). Email: address.
 * @param subject        email subject; ignored for WhatsApp
 * @param body           plain-text body; for a WhatsApp template it is a log summary of the parameters
 * @param attachments    files sent with an email; ignored for WhatsApp
 * @param templateName   approved WhatsApp template name; null sends free-form text
 * @param templateParams values for the template's body placeholders {{1}}, {{2}}, ... in order
 */
public record NotificationMessage(String recipient, String subject, String body, List<Attachment> attachments,
                                  String templateName, List<String> templateParams) {

    public NotificationMessage(String recipient, String subject, String body) {
        this(recipient, subject, body, List.of(), null, List.of());
    }

    public NotificationMessage(String recipient, String subject, String body, List<Attachment> attachments) {
        this(recipient, subject, body, attachments, null, List.of());
    }

    public NotificationMessage {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        templateParams = templateParams == null ? List.of() : List.copyOf(templateParams);
    }

    /** A WhatsApp template message. Parameters fill {{1}}, {{2}}, ... in the approved template, in order. */
    public static NotificationMessage template(String recipient, String templateName, List<String> params) {
        String summary = params == null || params.isEmpty() ? templateName : templateName + ": " + String.join(" | ", params);
        return new NotificationMessage(recipient, null, summary, List.of(), templateName, params);
    }

    /** A file attached to an email. */
    public record Attachment(String filename, String contentType, byte[] content) {
    }
}
