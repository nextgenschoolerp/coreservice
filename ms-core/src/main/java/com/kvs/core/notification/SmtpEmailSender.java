package com.kvs.core.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Email through any SMTP relay (Gmail, SES, SendGrid SMTP, the school's mail server). Configured by spring.mail.*. */
public class SmtpEmailSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(NotificationMessage message) {
        try {
            boolean multipart = !message.attachments().isEmpty();
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.recipient());
            helper.setSubject(message.subject() == null ? "" : message.subject());
            helper.setText(message.body(), false);
            for (NotificationMessage.Attachment file : message.attachments()) {
                helper.addAttachment(file.filename(), new ByteArrayResource(file.content()), file.contentType());
            }
            mailSender.send(mime);
            return "SMTP-" + UUID.randomUUID();
        } catch (MailException | MessagingException e) {
            throw new NotificationDispatchException("SMTP rejected the message: " + e.getMessage(), e);
        }
    }
}
