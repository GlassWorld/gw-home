package com.gw.api.config;

import com.gw.api.service.work.Microsoft365MailSender;
import com.gw.api.service.work.Microsoft365MailTokenClient;
import java.net.URI;
import java.time.Clock;
import java.util.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(WorkNotificationProperties.class)
public class WorkNotificationConfig {
    @Bean
    public Clock notificationClock() { return Clock.systemUTC(); }

    @Bean
    public JavaMailSender notificationMailSender(WorkNotificationProperties settings) {
        boolean microsoft=settings.getSmtpAuthMode()==WorkNotificationProperties.SmtpAuthMode.MICROSOFT365_OAUTH2;
        if (settings.isEnabled()) {
            URI url=URI.create(settings.getAppUrl());
            if (!settings.isReady() || url.getHost()==null || !java.util.Set.of("https","http").contains(url.getScheme())
                    || url.getRawQuery()!=null || url.getRawFragment()!=null || url.getUserInfo()!=null
                    || settings.getFrom().contains("\n") || settings.getFrom().contains("\r")
                    || settings.getFromName().contains("\n") || settings.getFromName().contains("\r")
                    || settings.getFromName().length()>200) {
                throw new IllegalStateException("메일 발송 환경설정과 앱 URL을 확인하세요.");
            }
            if (microsoft && (!"smtp.office365.com".equalsIgnoreCase(settings.getSmtpHost())
                    || settings.getSmtpPort()!=587 || !settings.isStarttls() || settings.isSsl()
                    || !settings.getFrom().equalsIgnoreCase(settings.getSmtpUsername()))) {
                throw new IllegalStateException("Microsoft 365 발신 계정·서버·STARTTLS 설정을 확인하세요.");
            }
        }
        JavaMailSenderImpl sender=microsoft
                ? new Microsoft365MailSender(new Microsoft365MailTokenClient(settings,notificationClock()))
                : new JavaMailSenderImpl();
        sender.setHost(settings.getSmtpHost()); sender.setPort(settings.getSmtpPort());
        sender.setUsername(settings.getSmtpUsername());
        if (!microsoft) sender.setPassword(settings.getSmtpPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties properties=sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth",Boolean.toString(!settings.getSmtpUsername().isBlank()));
        properties.setProperty("mail.smtp.starttls.enable",Boolean.toString(settings.isStarttls()));
        properties.setProperty("mail.smtp.starttls.required",Boolean.toString(settings.isStarttls()));
        properties.setProperty("mail.smtp.ssl.enable",Boolean.toString(settings.isSsl()));
        properties.setProperty("mail.smtp.ssl.checkserveridentity","true");
        properties.setProperty("mail.smtp.connectiontimeout","10000");
        properties.setProperty("mail.smtp.timeout","10000");
        properties.setProperty("mail.smtp.writetimeout","10000");
        properties.setProperty("mail.smtp.sendpartial","false");
        if (microsoft) {
            properties.setProperty("mail.smtp.auth.mechanisms","XOAUTH2");
            properties.setProperty("mail.smtp.auth.xoauth2.disable","false");
            properties.setProperty("mail.smtp.auth.login.disable","true");
            properties.setProperty("mail.smtp.auth.plain.disable","true");
            properties.setProperty("mail.smtp.auth.ntlm.disable","true");
            properties.setProperty("mail.smtp.auth.digest-md5.disable","true");
        }
        return sender;
    }
}
