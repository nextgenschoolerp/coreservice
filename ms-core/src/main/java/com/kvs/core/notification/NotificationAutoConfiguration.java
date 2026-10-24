package com.kvs.core.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers {@link NotificationDispatcher} with one sender per channel. A channel uses its real provider when
 * {@code kvs.notification.<channel>.provider} is {@code CLOUD_API} (WhatsApp) or {@code SMTP} (email), and logs otherwise.
 */
@AutoConfiguration
@EnableConfigurationProperties(NotificationAutoConfiguration.NotificationProperties.class)
public class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificationDispatcher.class)
    public NotificationDispatcher notificationDispatcher(NotificationProperties props,
                                                         ObjectProvider<RestClient.Builder> restClients,
                                                         ObjectProvider<JavaMailSender> mailSenders) {
        List<NotificationSender> senders = new ArrayList<>();

        NotificationProperties.WhatsAppProps wa = props.whatsapp();
        if (wa.provider() == Provider.CLOUD_API) {
            senders.add(new WhatsAppCloudSender(restClients.getObject(), new WhatsAppCloudSender.WhatsAppCloudSettings(
                    wa.accessToken(), wa.phoneNumberId(), wa.apiVersion(), wa.templateLanguage())));
        } else {
            senders.add(new LoggingNotificationSender(NotificationChannel.WHATSAPP));
        }

        NotificationProperties.EmailProps email = props.email();
        if (email.provider() == Provider.SMTP) {
            JavaMailSender mail = mailSenders.getIfAvailable();
            if (mail == null) {
                throw new IllegalStateException("kvs.notification.email.provider=SMTP needs spring.mail.host to be set");
            }
            senders.add(new SmtpEmailSender(mail, email.from()));
        } else {
            senders.add(new LoggingNotificationSender(NotificationChannel.EMAIL));
        }

        return new NotificationDispatcher(senders);
    }

    public enum Provider { LOG, CLOUD_API, SMTP }

    @ConfigurationProperties(prefix = "kvs.notification")
    public record NotificationProperties(WhatsAppProps whatsapp, EmailProps email) {

        public NotificationProperties {
            if (whatsapp == null) whatsapp = new WhatsAppProps(null, null, null, null, null);
            if (email == null) email = new EmailProps(null, null);
        }

        public record WhatsAppProps(Provider provider, String accessToken, String phoneNumberId, String apiVersion, String templateLanguage) {
            public WhatsAppProps {
                if (provider == null) provider = Provider.LOG;
                if (apiVersion == null) apiVersion = "v20.0";
                if (templateLanguage == null) templateLanguage = "en";
            }
        }

        public record EmailProps(Provider provider, String from) {
            public EmailProps {
                if (provider == null) provider = Provider.LOG;
            }
        }
    }
}
